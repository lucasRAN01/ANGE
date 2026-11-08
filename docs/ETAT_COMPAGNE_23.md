# Vérification ANGE 2.3

8 novembre 2026. APK personnel de débogage : `2.3.0-companion`, code 6, minSdk 26, target/compile SDK 35.

- Compilation APK et APK de tests réussie : Gradle 8.11.1, JDK 17, AGP 8.10.1, dépendances disponibles hors ligne.
- **42 tests JVM réussis** : politiques existantes, heures/dates et fuseaux à minuit, batterie inconnue, absence de faux déclenchements, contacts exacts/ambigus, rejet de codes de numérotation, navigation et douze poses.
- **21 tests instrumentaux réussis**, AVD API 35 x86_64 : mémoire, commandes, vraie inférence locale CPU, rendu OpenGL, réglages vocaux, heure/date/batterie via le contrôleur sans modèle ni serveur, contacts simulés et intention `ACTION_CALL` interceptée après confirmation, annulation et confirmation vocale textuelle, raccourcis persistants, atlas transparent, requêtes/réponses Gemini simulées et blocage sans consentement. Un faux générateur vérifie que les échanges d’appel et les anciennes valeurs du téléphone sont exclus du contexte en ligne.
- Signature de développement SHA-256 : `66e531ecb2c5cf24851e43eb8e3cf469662c23eef6bc9cb16c677ca29472b39d`, identique aux précédents APK locaux.
- Les **309 fichiers** Jarvis et la licence Android restent vérifiés identiques à la référence.

## Essais visuels sur émulateur

Accueil et quatre actions rapides vérifiés visuellement. Les nouvelles réponses sont amenées automatiquement dans la discussion. La commande d’heure affiche une valeur système avec la source **Téléphone · données système**, en mode Sur téléphone et en mode avion.

Navigation facultative : la commande **Va à l’accueil** a réellement ouvert `NexusLauncherActivity`. Un conflit avec Maps pour les apostrophes droites/courbes a été corrigé et couvert par test. Le service d’accessibilité a ensuite été désactivé sur l’AVD.

Mini-compagne réellement visible au-dessus du lanceur Android, avec service de premier plan microphone/specialUse. Déplacement à gauche/droite et salutation inspectés dans une vidéo native de **11,929944 s**, **1080 × 1920**, sans audio. Le bouton × a fermé le service ; son absence a été vérifiée. Un bref cycle extinction/réveil a également été exercé, sans le présenter comme une validation complète des écrans sécurisés Samsung.

Preuves : [compilation](verification/companion23-build.txt), [JVM](verification/companion23-unit.xml), [instrumentation](verification/companion23-instrumentation.txt), [résumé manuel](verification/companion23-manual.json), [accueil](verification/ange23-home.png), [superposition](verification/ange23-floating.png), [heure](verification/ange23-clock.xml).

## Limites des vérifications

Aucun appel réel composé ni SMS envoyé. Les essais d’appel utilisent des contacts simulés et interceptent l’intention Android : ils ne constituent pas un essai opérateur. Le vrai carnet du Galaxy A53, la dictée française, la voix audible, la double SIM, l’autonomie, la chauffe et les particularités Samsung restent à essayer sur ce téléphone.

Aucune clé Google personnelle utilisée ni conversation Gemini réelle effectuée. Les tests couvrent le format, le contexte, le consentement et des réponses simulées ; ils ne prouvent pas la disponibilité ou la qualité du fournisseur pour le compte utilisateur. La gratuité dépend de Google et d’un projet sans facturation, état que l’APK ne peut pas vérifier.

Le personnage humain est un atlas animé 2D au rendu 3D, pas un humain 3D articulé. La navigation est limitée aux commandes documentées. Les raccourcis sont explicitement enseignés, sans réentraînement du modèle. ANGE reste un assistant partiel en développement.
