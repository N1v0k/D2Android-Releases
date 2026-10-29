package com.winlator.d2;







final class D2InstallFailure {

    enum Action {

        AGAIN("install_again", true),

        KEYS("install_action_keys", true),

        PATCH("install_again", true),




        ANEW("install_start_anew", true),

        ACCESS("install_action_access", true),

        STORAGE("install_action_storage", false),

        LEAVE("install_leave", false);

        final String label;

        final boolean forward;

        Action(String label, boolean forward) {
            this.label = label;
            this.forward = forward;
        }
    }


    enum Kind {
        NO_ACCESS("install_fail_no_access", false, Action.ACCESS, Action.LEAVE),
        NO_SPACE("install_fail_no_space", false, Action.AGAIN, Action.STORAGE, Action.LEAVE),

        INSTALLER_NO_SPACE("install_fail_no_space", true, Action.AGAIN, Action.STORAGE, Action.LEAVE),
        NO_NETWORK("install_fail_no_network", false, Action.AGAIN, Action.LEAVE),
        REFUSED("install_fail_refused", false, Action.AGAIN, Action.LEAVE),
        DOWNLOAD_INCOMPLETE("install_fail_download_incomplete", false, Action.AGAIN, Action.LEAVE),
        DOWNLOAD_DAMAGED("install_fail_download_damaged", false, Action.AGAIN, Action.LEAVE),
        RUNTIME("install_fail_runtime", true, Action.AGAIN, Action.LEAVE),
        INSTALLER("install_fail_installer", true, Action.KEYS, Action.LEAVE),

        INSTALLER_DIALOG("install_fail_dialog", true, Action.AGAIN, Action.LEAVE),
        PATCH("install_fail_patch", false, Action.PATCH, Action.ANEW, Action.LEAVE),

        CHECK("install_fail_check", false, Action.ANEW, Action.LEAVE);

        final String text;

        final boolean discard;
        final Action[] actions;

        Kind(String text, boolean discard, Action... actions) {
            this.text = text;
            this.discard = discard;
            this.actions = actions;
        }
    }

    private D2InstallFailure() {}


    static Kind afterDownload(int percent) {
        return percent >= 100 ? Kind.DOWNLOAD_DAMAGED : Kind.DOWNLOAD_INCOMPLETE;
    }
}
