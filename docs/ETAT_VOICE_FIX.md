# Vérification du correctif vocal 2.2.1

Vérifications du 8 novembre 2026. Android : `versionCode=5`, `versionName=2.2.1-voice-fix`.

- Compilation locale réussie : APK, APK de tests instrumentaux et tests JVM, Gradle 8.11.1 / JDK 17 / SDK 35, dépendances disponibles hors ligne.
- **29 tests JVM réussis**, dont cinq nouveaux sur le routage vocal : consentement, codes 12/13, absence de boucle, refus de repli pour silence/permissions et tags régionaux.
- **10 tests instrumentaux réussis** sur notre émulateur API 35 x86_64 : les huit essais précédents (mémoire, commandes natives, vraie inférence CPU locale et rendu OpenGL) et deux nouveaux essais sur les paramètres vocaux Android, les constantes d’erreur, la révocation de l’autorisation et l’absence de réponse créée par le changement de consentement.
- Signature vérifiée par `apksigner`. SHA-256 du certificat de développement : `66e531ecb2c5cf24851e43eb8e3cf469662c23eef6bc9cb16c677ca29472b39d`, identique à nos APK locaux précédents.
- Les 309 fichiers de Jarvis restent identiques à la révision de référence ; licence intégrée Android identique. Le service PC n’a pas été modifié pour ce correctif.

Preuves : [compilation](verification/voice-fix-build.txt), [tests JVM](verification/voice-fix-unit.xml), [tests instrumentaux](verification/voice-fix-instrumentation.txt).

Limite : les tests de routage couvrent la politique de décision ; ils ne simulent pas un fournisseur vocal réel refusant le français puis réussissant sa transcription sur le service de secours. L’émulateur n’a pas servi à enregistrer de vraie voix. Le fonctionnement du microphone, du modèle vocal français, des téléchargements de langues et de la synthèse sur le Galaxy A53 doit être confirmé sur cet appareil. Aucun moteur STT français indépendant n’a été embarqué. Consulter [le guide de dépannage](RECONNAISSANCE_VOCALE.md).
