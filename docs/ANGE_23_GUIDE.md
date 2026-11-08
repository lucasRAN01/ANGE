# ANGE 2.3 — mini-compagne et commandes du téléphone

**Mise à jour 2.3.1 standard :** le service d’accessibilité a été supprimé. Les instructions d’activation de Navigation ANGE ci-dessous décrivent la version 2.3 historique et ne s’appliquent plus. Utiliser désormais `ANGE-Mobile-Standard-231-debug.apk`, code 7. Accueil, contacts/appels confirmés, mini-compagne, caméra et conversations restent disponibles. Voir [le guide Play Protect](PLAY_PROTECT.md) pour l’installation.

Cette version simplifie l’interface Android en quatre espaces : **ANGE**, **Actions**, **Mémoire**, **Réglages**. Elle ajoute une mini-compagne inspirée de la photo fournie, des appels par nom de contact, une navigation facultative et des raccourcis appris explicitement. L’heure, la date et la batterie sont lues dans Android avant tout appel à un modèle, dans les quatre modes.

## Mettre à jour et tester les fonctions locales

Installer `ANGE-Mobile-Companion-23-debug.apk` par-dessus nos APK locaux précédents, sans désinstallation ni effacement des données. Version 2.3.0-companion, code 6 ; même signature de développement. Le modèle déjà installé, les réglages et la mémoire doivent être conservés avec une mise à jour compatible.

1. Ouvrir ANGE : le nouveau personnage et les quatre raccourcis apparaissent. Le toucher déclenche une salutation ; le glisser déplace le personnage dans sa scène.
2. **Heure** : demander « Quelle heure est-il ? » puis « Quel jour sommes-nous ? ». La source indiquée est **Téléphone · données système**. Le fuseau et la date sont ceux du téléphone ; cette fonction n’effectue pas de conversion de ville ou de date relative.
3. **Batterie** : « Combien de batterie me reste-t-il ? ». La valeur provient de BatteryManager, sans réponse générée. Android peut être incapable de fournir cette valeur sur certains appareils ; ANGE le signale.
4. **Caméra** : « Ouvre appareil photo ». ANGE demande l’ouverture à l’application caméra ; elle ne photographie pas sans votre intervention.
5. **Appels** : « Appelle Babe ». Autoriser **Contacts** à la demande. ANGE recherche localement ce nom, sans envoyer le carnet d’adresses au modèle. Si plusieurs personnes ou numéros correspondent, choisir celui voulu. Autoriser **Téléphone** à la demande, puis vérifier le nom et le numéro et appuyer sur **Appeler**. L’intention Android `ACTION_CALL` est alors envoyée : il s’agit d’un véritable appel, contrairement à l’ancien simple composeur. Une double SIM peut entraîner un choix supplémentaire dans Android. Les appels d’urgence ou les restrictions de l’opérateur restent gérés par le système. Tester de préférence avec un proche averti ; les essais automatiques du projet ne composent aucun numéro.
6. **Confirmation vocale** : dans la fenêtre d’appel, toucher **Confirmer à la voix**, puis dire exactement « Confirme l’appel ». « Annule » annule l’appel en attente. Une phrase de confirmation sans appel en attente ne lance rien. Annuler dans ANGE doit éviter tout lancement de l’appel. « Compose 034… » conserve l’ancien comportement de préparation dans le composeur.
7. **Applications** : « Ouvre WhatsApp » ou choisir une application dans **Actions**. Le nom doit correspondre à une application visible et installée. Les profils professionnels et les applications masquées peuvent ne pas apparaître.

Les boutons permettent de vérifier les actions séparément du microphone. Pour parler, appuyer sur le micro, choisir FR dans Réglages et autoriser le microphone. Le [dépannage des codes 12/13](RECONNAISSANCE_VOCALE.md) reste applicable. Le modèle vocal français dépend du service Android installé ; aucun STT français indépendant n’a été embarqué.

## Le personnage au-dessus des applications

1. Dans **Réglages → Mini-compagne**, ouvrir **Autoriser l’affichage flottant** et activer l’autorisation pour ANGE dans Android.
2. Revenir dans ANGE, choisir éventuellement **Petites balades**, puis **Activer la mini-compagne**. Autoriser le microphone pour Parler et les notifications pour voir sa commande d’arrêt.
3. Aller dans une autre application : le petit personnage reste visible. Le glisser du doigt le déplace ; il rejoint un bord au relâchement. Si les balades sont activées, il traverse périodiquement l’écran lorsqu’il est au repos.
4. **Parler** lance une écoute ponctuelle et affiche la réponse dans sa bulle. **ANGE** ouvre l’application complète ; **×** ferme la mini-compagne. Les confirmations d’appels ou les choix de contacts rouvrent l’application pour vérification.
5. Le bouton **Arrêter** de la notification ferme le service. Verrouiller l’écran arrête l’écoute et masque le personnage ; le déverrouillage peut le réafficher, sans relancer automatiquement le micro. Pas de lancement au redémarrage du téléphone.

Le service est déclaré visible, de type microphone et specialUse selon les autorisations. Android/Samsung peut limiter ou arrêter ce mode, notamment sous pression mémoire ou économie d’énergie. Les écrans sécurisés peuvent masquer les superpositions. Le personnage flottant a sa propre session courte de discussion ; l’historique de cette session n’est pas fusionné avec celui de l’écran principal. Le mode et les autorisations réseau de session sont copiés à son activation ; utiliser Actualiser après un changement. Sa mémoire structurée et ses raccourcis restent les fichiers privés communs de l’application.

## Navigation facultative

**Réglages → Activer Navigation ANGE** ouvre les réglages d’accessibilité. Activer **Navigation ANGE** seulement si vous souhaitez ces commandes : **Retour**, **Va à l’accueil**, **Applications récentes**, **Descends**, **Monte**. Les deux dernières cherchent une zone défilable dans la structure d’accessibilité de l’écran courant.

Ce service ne clique pas sur des boutons quelconques, ne lit pas l’écran à l’IA, ne capture pas d’images et n’enregistre pas les événements. Il ne sait pas tout faire dans toutes les applications. Certaines vues ne publient pas de zone défilable. Désactiver le service dans Android retire ces capacités.

## Apprendre une action déjà connue

Dans **Actions → Apprenez-moi un raccourci**, associer une phrase à une commande prise en charge, par exemple :

| Quand je dis | Commande retenue |
| --- | --- |
| Mode discret | Volume à 20 % |
| Je prends une photo | Ouvre appareil photo |
| Mon appel préféré | Appelle Babe |

Après une action, **Utiliser ma dernière action** peut remplir la commande. Les raccourcis sont enregistrés localement, persistent après fermeture et peuvent être oubliés. Un appel retenu conserve sa confirmation. Limite : 30 raccourcis simples ; pas de séquence arbitraire de clics, d’observation passive de vos gestes ou de réentraînement des poids du modèle.

## Une discussion plus riche

**Sur téléphone** conserve Qwen3 0.6B, gratuit après téléchargement. Ce modèle est très petit et peut mal répondre ou se répéter. Les commandes système sont désormais séparées de cette conversation ; leurs anciennes valeurs ne sont pas réinjectées dans le contexte génératif.

**En ligne** utilise Google Gemini 3.1 Flash-Lite, avec une clé personnelle Google AI Studio à saisir uniquement dans l’application. Utiliser un projet **sans facturation activée** ; le niveau gratuit est soumis aux quotas et aux conditions de Google. Une clé liée à un projet payant peut entraîner des frais : ANGE ne peut pas vérifier l’état du compte. Aucun abonnement n’est créé et aucun modèle payant de secours n’est appelé automatiquement.

Après activation explicite pour la session, les questions et jusqu’à douze messages de conversation précédents sont envoyés à Google. La clé est chiffrée avec Android Keystore. Les contacts, écrans, raccourcis et souvenirs structurés Jarvis ne sont pas joints au contexte ; les données personnelles que vous écrivez vous-même dans une discussion peuvent être transmises. Google indique que les échanges du niveau gratuit peuvent servir à améliorer ses produits. Voir [l’information complète](CONVERSATION_EN_LIGNE_PROPOSEE.md).

Le mode en ligne peut produire une conversation beaucoup plus riche qu’un modèle 0.6B ; aucune équivalence de qualité avec ChatGPT ou Claude n’est garantie. Le service peut refuser une requête, imposer un quota ou devenir indisponible. La qualité réelle exige un essai avec votre configuration. Les essais de développement ne prétendent pas avoir testé une vraie conversation Gemini sans clé.

## Limites du personnage

La mini-ANGE humaine est une illustration au rendu 3D, animée en **2D par douze poses**, avec respiration, clignement, salutation, écoute, réflexion, marche latérale et alternance de bouche pendant la synthèse. Elle n’est pas un humain 3D articulé, une reproduction exacte de la personne ni un clonage de sa voix. Le robot procédural réellement 3D et l’ancien portrait restent disponibles dans Réglages.

L’[étude d’animation](ETUDE_ANIMATION_ANGE.md) précise l’implémentation et ce qu’il faudrait encore développer pour une animation humaine complète.
