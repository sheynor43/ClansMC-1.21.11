package io.github.sheynor43.clans;

import io.github.sheynor43.clans.api.ClansAPI;
import io.github.sheynor43.clans.chat.ChatListener;
import io.github.sheynor43.clans.chat.ClanChatService;
import io.github.sheynor43.clans.command.AdminCommand;
import io.github.sheynor43.clans.command.ChatCommand;
import io.github.sheynor43.clans.command.ClanCommand;
import io.github.sheynor43.clans.config.ConfigManager;
import io.github.sheynor43.clans.config.Settings;
import io.github.sheynor43.clans.config.StorageType;
import io.github.sheynor43.clans.dialog.CreateDialogManager;
import io.github.sheynor43.clans.glow.GlowManager;
import io.github.sheynor43.clans.gui.MenuManager;
import io.github.sheynor43.clans.hook.AuthMeHook;
import io.github.sheynor43.clans.hook.EconomyProvider;
import io.github.sheynor43.clans.hook.PlaceholderApiHook;
import io.github.sheynor43.clans.hook.VaultEconomyProvider;
import io.github.sheynor43.clans.lang.LangManager;
import io.github.sheynor43.clans.listener.BossXpListener;
import io.github.sheynor43.clans.listener.ConnectionListener;
import io.github.sheynor43.clans.listener.FriendlyFireListener;
import io.github.sheynor43.clans.service.AllyRequestService;
import io.github.sheynor43.clans.service.BankService;
import io.github.sheynor43.clans.service.BossXpService;
import io.github.sheynor43.clans.service.ClanBroadcaster;
import io.github.sheynor43.clans.service.ClanCreationService;
import io.github.sheynor43.clans.service.ClanManager;
import io.github.sheynor43.clans.service.InviteService;
import io.github.sheynor43.clans.storage.ClanStorage;
import io.github.sheynor43.clans.storage.MysqlClanStorage;
import io.github.sheynor43.clans.storage.SqliteClanStorage;
import io.github.sheynor43.clans.tab.TabManager;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * Entry point. Wires every service through constructors (no static singletons
 * beyond this class), registers listeners and Brigadier commands, and loads the
 * clan cache asynchronously so the main thread never blocks on the database.
 */
public final class ClansPlugin extends JavaPlugin {

    private ConfigManager configManager;
    private LangManager langManager;
    private ClanStorage storage;
    private ClanManager clanManager;
    private ClanBroadcaster broadcaster;
    private InviteService inviteService;
    private AllyRequestService allyRequestService;
    private ClanCreationService creationService;
    private BankService bankService;
    private GlowManager glowManager;
    private TabManager tabManager;
    private CreateDialogManager dialogManager;
    private ClanChatService clanChatService;
    private BossXpService bossXpService;
    private MenuManager menuManager;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.langManager = new LangManager(this);
        this.langManager.load(settings());

        Supplier<Settings> settings = this::settings;

        this.storage = buildStorage();
        this.clanManager = new ClanManager(storage, settings, getLogger());
        this.broadcaster = new ClanBroadcaster(langManager);

        EconomyProvider economy = VaultEconomyProvider.tryCreate();
        if (settings().bankEnabled() && (economy == null || !economy.isEnabled())) {
            getLogger().warning("bank.enabled is true but no Vault economy was found; the clan bank is disabled.");
        }
        this.bankService = new BankService(settings, clanManager, economy);

        this.inviteService = new InviteService(settings);
        this.allyRequestService = new AllyRequestService(settings);
        this.creationService = new ClanCreationService(settings, clanManager);
        AuthMeHook authMe = new AuthMeHook();
        this.tabManager = new TabManager(this, settings, clanManager, authMe);
        this.glowManager = new GlowManager(settings, clanManager);
        this.dialogManager = new CreateDialogManager(this, settings, langManager, creationService, tabManager);
        this.clanChatService = new ClanChatService(this, settings, langManager, broadcaster);
        this.bossXpService = new BossXpService(this, clanManager, storage, langManager, broadcaster, settings);
        this.menuManager = new MenuManager(this);

        // Initialise storage BEFORE registering commands: if the database is
        // unreachable we disable cleanly, leaving no half-registered ("ghost")
        // commands that would crash with a closed class loader when used.
        if (!initStorage()) {
            return;
        }

        registerListeners();
        registerCommands();
        registerHooks(economy != null);
        scheduleTasks();
        getServer().getServicesManager().register(ClansAPI.class, clanManager, this, ServicePriority.Normal);

        tabManager.applyAll();
        for (Player player : Bukkit.getOnlinePlayers()) {
            glowManager.applyForJoin(player);
        }
        getLogger().info("ClansMC enabled (storage: " + settings().storageType() + ", "
                + clanManager.getClans().size() + " clan(s) loaded).");
    }

    @Override
    public void onDisable() {
        if (bossXpService != null) {
            bossXpService.clear();
        }
        if (glowManager != null) {
            glowManager.clearAll();
        }
        if (storage != null) {
            storage.shutdown();
        }
        getLogger().info("ClansMC disabled.");
    }

    private ClanStorage buildStorage() {
        if (settings().storageType() == StorageType.SQLITE) {
            File file = new File(getDataFolder(), settings().sqliteFile());
            return new SqliteClanStorage(file, getClassLoader(), getLogger());
        }
        return new MysqlClanStorage(settings(), getClassLoader(), getLogger());
    }

    /**
     * Opens the pool, runs migrations and loads the cache synchronously (once, at
     * startup). All gameplay operations stay asynchronous; only this one-time
     * bootstrap blocks, so a misconfigured database fails fast and cleanly.
     *
     * @return {@code true} on success; {@code false} after disabling the plugin.
     */
    private boolean initStorage() {
        try {
            storage.init().get(30, TimeUnit.SECONDS);
            clanManager.loadAll().get(30, TimeUnit.SECONDS);
            return true;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failStorage(ex);
        } catch (ExecutionException | TimeoutException ex) {
            return failStorage(ex);
        }
    }

    private boolean failStorage(Exception ex) {
        Throwable cause = rootCause(ex);
        getLogger().severe("Could not initialise " + settings().storageType() + " storage: " + cause.getMessage());
        getLogger().severe("Fix storage settings in plugins/ClansMC/config.yml (host, database, user, password), "
                + "or set 'storage.type: SQLITE' for zero-setup local storage, then restart. Disabling ClansMC.");
        storage.shutdown();
        getServer().getPluginManager().disablePlugin(this);
        return false;
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return current;
    }

    private void registerListeners() {
        var pm = getServer().getPluginManager();
        Supplier<Settings> settings = this::settings;
        pm.registerEvents(new FriendlyFireListener(settings, clanManager), this);
        pm.registerEvents(new BossXpListener(settings, bossXpService), this);
        pm.registerEvents(new ConnectionListener(this, settings, clanManager, storage, tabManager, glowManager,
                dialogManager, clanChatService, inviteService, broadcaster), this);
        pm.registerEvents(new ChatListener(this, dialogManager, clanChatService, clanManager, langManager), this);
        pm.registerEvents(menuManager, this);
    }

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            final Commands registrar = event.registrar();
            registrar.register(new ClanCommand(this).build(), "ClansMC main command", List.of());
            registrar.register(new ChatCommand(this).build(), "Clan chat message", List.of());
            registrar.register(new AdminCommand(this).build(), "ClansMC administration", List.of("ca"));
        });
    }

    private void registerHooks(boolean vaultPresent) {
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderApiHook(this::settings, clanManager, bankService, getPluginMeta().getVersion()).register();
            getLogger().info("Registered PlaceholderAPI expansion.");
        }
        if (vaultPresent) {
            getLogger().info("Hooked into Vault economy.");
        }
    }

    private void scheduleTasks() {
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            inviteService.purgeExpired();
            allyRequestService.purgeExpired();
            bossXpService.purgeStale();
        }, 1200L, 1200L);

        int interval = settings().tabUpdateInterval();
        if (interval > 0) {
            Bukkit.getScheduler().runTaskTimer(this, tabManager::applyAll, interval, interval);
        }
    }

    /** Reloads config and languages, then re-applies tab names. Storage changes need a restart. */
    public void reloadEverything() {
        configManager.load();
        langManager.load(settings());
        tabManager.applyAll();
    }

    // ---- accessors ----------------------------------------------------------

    public Settings settings() {
        return configManager.settings();
    }

    public LangManager lang() {
        return langManager;
    }

    public ClanManager clans() {
        return clanManager;
    }

    public ClanBroadcaster broadcaster() {
        return broadcaster;
    }

    public InviteService invites() {
        return inviteService;
    }

    public AllyRequestService allyRequests() {
        return allyRequestService;
    }

    public ClanCreationService creation() {
        return creationService;
    }

    public BankService bank() {
        return bankService;
    }

    public GlowManager glow() {
        return glowManager;
    }

    public TabManager tab() {
        return tabManager;
    }

    public CreateDialogManager dialog() {
        return dialogManager;
    }

    public ClanChatService clanChat() {
        return clanChatService;
    }

    public MenuManager menus() {
        return menuManager;
    }
}
