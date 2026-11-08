# Vérification de la version standard 2.3.1

8 novembre 2026. Version `2.3.1-standard`, code 7, paquet inchangé `com.ange.mobile`.

- Compilation locale APK, tests JVM et APK d’instrumentation réussie, Gradle 8.11.1 / JDK 17 / SDK 35.
- **42 tests JVM**, zéro erreur/échec.
- **23 tests instrumentaux**, zéro échec, sur notre AVD API 35 x86_64. Les 21 essais 2.3 sont conservés ; deux essais supplémentaires inspectent les permissions/services réellement installés et vérifient que l’accueil utilise une intention publique Android, tandis que Retour/Récentes/défilement interapplications ne sont pas exécutés.
- Inspection du manifeste compilé via `aapt` : absence de service d’accessibilité, de BIND_ACCESSIBILITY_SERVICE, de NotificationListener, READ_SMS et RECEIVE_SMS. Vérification des DEX : l’ancienne classe `AngeNavigationService` est absente de l’APK.
- Signature vérifiée avec `apksigner`, SHA-256 du certificat : `66e531ecb2c5cf24851e43eb8e3cf469662c23eef6bc9cb16c677ca29472b39d`, identique aux anciens APK locaux. Il s’agit toujours d’une signature de développement, pas d’une approbation Google.

Preuves : [compilation](verification/standard231-build.txt), [tests JVM](verification/standard231-unit.xml), [instrumentation](verification/standard231-instrumentation.txt), [manifeste compilé](verification/standard231-apk-manifest.txt).

Cette version réduit réellement les capacités de l’application. Elle ne modifie pas Play Protect, ne change pas de paquet pour masquer l’application et n’essaie pas de réactiver le service retiré. Mini-compagne, affichage flottant volontaire, commandes publiques Android, contacts/appels confirmés et modes de discussion sont conservés.

**L’acceptation Play Protect sur le Galaxy A53 n’a pas été vérifiée.** Installer sur un émulateur de développement ne valide pas ce verdict. Les limites 2.3 concernant la dictée/voix du téléphone réel, le réseau Google non testé avec une vraie clé et les appels réels non effectués restent applicables.

Voir [les instructions d’installation et le recours officiel si nécessaire](PLAY_PROTECT.md). Garder les protections du téléphone actives.
