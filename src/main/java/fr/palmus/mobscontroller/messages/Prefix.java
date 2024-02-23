package fr.palmus.mobscontroller.messages;


import fr.palmus.mobscontroller.MobsController;

/**
 * This class represents a "Prefix" and provides a method to retrieve the prefix based on a given "PrefixLevel" state.
 * The class also initializes three static final variables: "prefixNormal," "prefixGood," and "prefixError," which are obtained from a configuration file.
 */
public class Prefix {

    private static final MobsController main;

    static {
        main = MobsController.getInstance();
    }
    private static final String prefixNormal = main.getConfig().getString("prefix.ok").replace("&", "§");
    private static final String prefixGood = main.getConfig().getString("prefix.good").replace("&", "§");
    private static final String prefixError = main.getConfig().getString("prefix.error").replace("&", "§");

    /**
     * Renvoi le préfixe devant être devant "tous" les messages envoyés aux joueurs
     */
    public static String getPrefix(PrefixLevel state) {

        if (state == PrefixLevel.ERROR) {
            return prefixError;
        }

        if (state == PrefixLevel.GOOD) {
            return prefixGood;
        }

        return prefixNormal;
    }
}
