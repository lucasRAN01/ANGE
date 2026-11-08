# Vérifications manuelles — 2.3.2 mini

Appareil : émulateur Android 15 dédié, application com.ange.mobile, versionCode 8. Aucun téléphone personnel connecté.

- Mise à jour par installation `-r` : réussie, sans effacement des données.
- Icônes ouvrir/message/voix/options : barre visible, personnage réduit, pas de consigne textuelle au repos.
- Menu flottant : affiche Masquer les commandes et Fermer la mini-ANGE ; masquage vérifié ; toucher le personnage restaure la barre.
- Icône message depuis Réglages : ouvre ANGE/discussion et place le focus dans le champ avec clavier Android ; pas de plantage FocusRequester.
- Capture : premier consentement annulé, aucun dossier/fichier créé. Nouvelle commande et nouvel accord système, PNG 1080×1920 créé dans Pictures/ANGE ; PNG ouvert et inspecté. Personnage absent de la capture puis réaffiché ; service MediaProjection arrêté après l’image, service flottant toujours actif. Aucune image envoyée à un modèle.
- Autorisation de verrouillage : fenêtre Android réellement affichée après correction du lancement via MainActivity (Android refuse ADD_DEVICE_ADMIN avec NEW_TASK). Liste affichée : Lock the screen uniquement. Annulation vérifiée. Activation volontaire sur l’émulateur puis commande « Verrouille le téléphone » : état Android `mWakefulness=Dozing`, administrateur présent avec seulement `force-lock`. Réveil réussi. Retrait du droit depuis le bouton d’ANGE vérifié : administrateur et force-lock absents de dumpsys device_policy après révocation.
- Signature vérifiée avec apksigner : SHA-256 66e531ecb2c5cf24851e43eb8e3cf469662c23eef6bc9cb16c677ca29472b39d, identique aux APK locaux précédents. minSdk 26, targetSdk 35.
- Aucun service d’accessibilité, lecture/réception SMS ou écoute des notifications ajouté. XML administrateur limité à force-lock ; composants de capture privés.
- Snapshot Jarvis : 309 fichiers originaux et licence identiques à la révision épinglée, vérification locale réussie.

Limites : pas de validation sur Galaxy A53, de mesure de continuité sonore (émulateur sans audio), de reconnaissance française réelle ou de flash matériel. Pas de conversation Gemini réelle ni de test Play Protect. Une alerte « System UI isn’t responding » de l’émulateur a été fermée pendant les essais ; les résultats ne constituent pas une certification de stabilité Samsung. Les tests natifs couvrent la session partagée et les ouvertures, avec sorties sensibles interceptées plutôt que de vrais appels.

Bilan automatisé final : 47 tests JVM et 30 tests instrumentés réussis, aucun test ignoré annoncé. Build debug et APK de test compilés. Preuves brutes jointes.
