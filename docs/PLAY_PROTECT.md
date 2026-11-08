# Installation bloquée par Play Protect — version standard 2.3.1

La version actuelle **2.3.2 mini**, code 8, garde le retrait de l’accessibilité, des SMS et de l’écoute des notifications. Elle ajoute des accès facultatifs distincts : Caméra pour la lampe ; administrateur Android limité à `force-lock` pour verrouiller ; consentement de capture MediaProjection à chaque image. Ces capacités sont documentées dans [le guide actuel](MINI_232.md). Elles ne garantissent pas une acceptation par Play Protect.

La capture montre le message « Cette appli peut demander l’accès à des données sensibles… ». La documentation Google associe ce message aux APK provenant notamment d’un navigateur ou gestionnaire de fichiers et déclarant certaines capacités sensibles : accessibilité, lecture/réception des SMS ou écoute des notifications.

ANGE 2.3 déclarait un service d’accessibilité pour Retour, Accueil, Récentes et le défilement. Cela correspond à un critère documenté de blocage. Nous n’avons pas accès au verdict interne de Google concernant cet APK et ne prétendons pas que la capture constitue à elle seule un diagnostic exhaustif de sécurité.

## Modification réelle dans la version standard

La version **2.3.1-standard**, code **7**, supprime le service d’accessibilité, sa déclaration, son code et sa configuration. Elle ne peut plus lire la structure d’écran des autres applications, exécuter Retour/Récentes ou faire défiler leur contenu. La commande Accueil utilise désormais l’intention publique Android `ACTION_MAIN` / `CATEGORY_HOME`.

L’interface n’offre plus de bouton d’activation de ce service. Les anciennes commandes de navigation non disponibles sont refusées explicitement. Aucun renommage de paquet, camouflage d’autorisation ou modification de Play Protect n’est effectué. L’identifiant `com.ange.mobile` et la signature de développement sont conservés.

Restent disponibles : mini-compagne et affichage flottant volontaire, microphone sur appui, appels confirmés avec Contacts/Téléphone demandés à l’usage, caméra, lancement d’applications, heure/date/batterie, mémoire, raccourcis et modes de conversation. `POST_NOTIFICATIONS` sert à afficher la notification du service ; cela ne donne pas accès à la lecture des notifications d’autres applications. Aucun accès READ_SMS/RECEIVE_SMS ou NotificationListener n’est ajouté.

## Sur le téléphone

1. Fermer l’alerte et ne plus retenter l’ancien APK 2.3 avec ce message.
2. Télécharger **ANGE-Mobile-Mini-232-debug.apk** et tenter sa mise à jour **sans désinstaller** la version déjà présente. Ce nouvel APK garde la même signature ; un échec d’installation ne justifie pas d’effacer vos données ou le modèle.
3. Garder Play Protect actif et laisser Google analyser l’application si cette option est proposée.
4. Si le blocage persiste, arrêter les tentatives et conserver le message exact. Le propriétaire du projet pourra demander une révision via le recours Play Protect officiel, avec l’APK, sa version, sa signature et son empreinte. Ne pas contourner la protection ni désactiver les protections du téléphone.

La réduction des capacités répond au conseil Google d’utiliser seulement les autorisations nécessaires. Elle ne constitue pas une certification Google ni une garantie d’acceptation. Les vérifications locales de compilation, de manifeste et d’émulateur ne remplacent pas l’analyse Play Protect sur le Galaxy A53.

Sources : [message et recommandations officiels](https://developers.google.com/android/play-protect/warning-dev-guidance#app_blocked_to_protect_your_device), [révision du classement Play Protect](https://support.google.com/googleplay/android-developer/answer/2992033).
