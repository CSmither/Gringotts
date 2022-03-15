package org.gestern.gringotts.accountholder;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.gestern.gringotts.event.VaultCreationEvent;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages creating various types of AccountHolder centrally.
 *
 * @author jast
 */
public class AccountHolderFactory implements Iterable<AccountHolderProvider> {

    private final Map<String, AccountHolderProvider> accountHolderProviders = new LinkedHashMap<>();

    /**
     * Instantiates a new Account holder factory.
     */
    public AccountHolderFactory() {
        // linked HashMap maintains iteration order -> prefer player to be checked first
        accountHolderProviders.put("player", new PlayerAccountHolderProvider());

        // TODO support banks
        // TODO support virtual accounts
    }

    /**
     * Get an account holder with automatically determined type, based on the owner's id.
     *
     * @param owner name of the account holder
     * @return account holder for the given owner name, or null if none could be determined
     */
    public AccountHolder get(String owner) {
        for (AccountHolderProvider provider : accountHolderProviders.values()) {
            AccountHolder accountHolder = provider.getAccountHolder(owner);

            if (accountHolder != null) {
                return accountHolder;
            }
        }

        return null;
    }

    /**
     * Get an account holder with automatically determined type, based on the owner's id.
     *
     * @param uuid the user id
     * @return account holder for the given owner name, or null if none could be determined
     */
    public AccountHolder get(UUID uuid) {
        for (AccountHolderProvider provider : accountHolderProviders.values()) {
            AccountHolder accountHolder = provider.getAccountHolder(uuid);

            if (accountHolder != null) {
                return accountHolder;
            }
        }

        return null;
    }

    /**
     * Get an account holder with automatically determined type, based on the owner's id.
     *
     * @param player the user id
     * @return account holder for the given owner name, or null if none could be determined
     */
    public AccountHolder get(OfflinePlayer player) {
        for (AccountHolderProvider provider : accountHolderProviders.values()) {
            AccountHolder accountHolder = provider.getAccountHolder(player);

            if (accountHolder != null) {
                return accountHolder;
            }
        }

        return null;
    }

    /**
     * Get an account holder of known type.
     *
     * @param type  type of the account
     * @param owner name of the account holder
     * @return account holder of given type with given owner name, or null if none could be determined or type is not supported.
     */
    public AccountHolder get(String type, String owner) {
        AccountHolderProvider provider = accountHolderProviders.get(type);
        AccountHolder accountHolder = null;

        if (provider != null) {
            accountHolder = provider.getAccountHolder(owner);
        }

        return accountHolder;
    }

    /**
     * Register account holder provider.
     *
     * @param type     the type
     * @param provider the provider
     */
    public void registerAccountHolderProvider(String type, AccountHolderProvider provider) {
        accountHolderProviders.put(type, provider);
    }

    /**
     * Gets provider.
     *
     * @param type the type
     * @return the provider
     */
    public Optional<AccountHolderProvider> getProvider(VaultCreationEvent.Type type) {
        return this.getProvider(type.getId());
    }

    /**
     * Gets provider.
     *
     * @param type the type
     * @return the provider
     */
    public Optional<AccountHolderProvider> getProvider(String type) {
        return Optional.ofNullable(this.accountHolderProviders.getOrDefault(type, null));
    }

    /**
     * Returns an iterator over elements of type {@code T}.
     *
     * @return an Iterator.
     */
    @Override
    public Iterator<AccountHolderProvider> iterator() {
        return accountHolderProviders.values().iterator();
    }

    private static class PlayerAccountHolderProvider implements AccountHolderProvider {

        @Override
        public AccountHolder getAccountHolder(String uuidOrName) {
            try {
                return getAccountHolder(UUID.fromString(uuidOrName));
            } catch (IllegalArgumentException ignored) {
                // don't use getOfflinePlayer(String) because that will do a blocking web request
                // rather iterate this array, should be quick enough
                for (OfflinePlayer p : Bukkit.getOfflinePlayers()) {
                    if (uuidOrName.equals(p.getName())) {
                        return getAccountHolder(p);
                    }
                }

                return null;
            }
        }

        @Override
        public AccountHolder getAccountHolder(UUID uuid) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

            //noinspection ConstantConditions
            if (player != null) {
                return getAccountHolder(player);
            } else {
                return null;
            }
        }

        @Override
        public AccountHolder getAccountHolder(OfflinePlayer player) {
            // if this player has ever played on the server, they are a legit account holder
            if (player.isOnline() || player.hasPlayedBefore()) {
                return new PlayerAccountHolder(player);
            } else {
                return null;
            }
        }

        /**
         * Gets type.
         *
         * @return the type
         */
        @Override
        public VaultCreationEvent.Type getType() {
            return VaultCreationEvent.Type.PLAYER;
        }

        /**
         * Gets account names.
         *
         * @return the account names
         */
        @Override
        public Set<String> getAccountNames() {
            return Stream.of(Bukkit.getOfflinePlayers())
                    .map(OfflinePlayer::getName)
                    .collect(Collectors.toSet());
        }
    }

}
