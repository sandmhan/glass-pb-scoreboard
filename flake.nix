{
  description = "Reproducible Android development environment for Glass Pickleball Scoreboard";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs =
    { nixpkgs, ... }:
    let
      systems = [ "x86_64-linux" ];
      forAllSystems = nixpkgs.lib.genAttrs systems;
      mkPkgs =
        system:
        import nixpkgs {
          inherit system;
          config = {
            allowUnfreePredicate =
              package:
              let
                name = nixpkgs.lib.getName package;
              in
              builtins.any (prefix: nixpkgs.lib.hasPrefix prefix name) [
                "android"
                "build-tools"
                "cmdline-tools"
                "platform-tools"
                "platforms"
                "tools"
              ];
            android_sdk.accept_license = true;
          };
        };
      androidFor =
        pkgs:
        pkgs.androidenv.composeAndroidPackages {
          buildToolsVersions = [ "34.0.0" ];
          platformVersions = [ "34" ];
          includeCmake = false;
          includeEmulator = false;
          includeNDK = false;
          includeSystemImages = false;
        };
    in
    {
      devShells = forAllSystems (
        system:
        let
          pkgs = mkPkgs system;
          android = androidFor pkgs;
          androidSdk = android.androidsdk;
          androidHome = "${androidSdk}/libexec/android-sdk";
        in
        {
          default = pkgs.mkShellNoCC {
            name = "glass-pb-scoreboard";

            packages = [
              androidSdk
              android.platform-tools
              pkgs.deadnix
              pkgs.git
              pkgs.jdk17
              pkgs.nixfmt
              pkgs.python3
              pkgs.statix
            ];

            ANDROID_AAPT2_FROM_MAVEN_OVERRIDE = "${androidHome}/build-tools/34.0.0/aapt2";
            ANDROID_HOME = androidHome;
            ANDROID_SDK_ROOT = androidHome;
            JAVA_HOME = pkgs.jdk17.home;
            LANG = "C.UTF-8";
            LC_ALL = "C.UTF-8";

            shellHook = ''
              printf 'sdk.dir=%s\n' "$ANDROID_HOME" > local.properties
            '';
          };
        }
      );

      packages = forAllSystems (
        system:
        let
          pkgs = mkPkgs system;
        in
        {
          android-sdk = (androidFor pkgs).androidsdk;
          default = (androidFor pkgs).androidsdk;
        }
      );

      formatter = forAllSystems (system: (mkPkgs system).nixfmt);
    };
}
