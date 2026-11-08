# Fonctions du téléphone à autoriser explicitement

> Version actuelle 2.3.2 : voir [MINI_232.md](MINI_232.md). Mini-compagne unique, ancien robot/portrait alternatif retirés. Le service d’accessibilité des versions précédentes reste supprimé. Caméra pour la lampe, verrouillage facultatif limité et capture avec consentement Android sont les nouveaux accès distincts. Les descriptions historiques ci-dessous ne sont pas toutes des fonctions actuelles.


Projet de fonctions pour votre appareil personnel, désactivées avant vos autorisations Android.

1. **Contacts et appel par nom** : après « Appelle Babe », lire les noms et numéros des contacts localement, présenter les correspondances si le nom est ambigu, puis montrer le destinataire et le numéro avant de lancer un véritable appel avec votre confirmation. Permissions Android : Contacts et Téléphone. Aucun carnet d’adresses envoyé au modèle IA.
2. **Mini-compagne flottante** : personnage visible au-dessus des applications, déplaçable au toucher, avec bouton Parler et bouton Fermer. Activation volontaire depuis ANGE, autorisation Android « Apparaître par-dessus les autres applications », notification persistante avec Arrêter. Le microphone ne démarre qu’en appuyant sur Parler ou en activant une discussion vocale ; pas de démarrage au redémarrage du téléphone. Une permission micro est nécessaire et un service visible assure la session quand l’application principale est en arrière-plan.
3. **Navigation Android facultative** : Retour, Accueil, Applications récentes et défilement vers le haut/bas sur commande explicite. Activation manuelle dans les réglages d’accessibilité. Ce service peut accéder à la structure de l’écran pour trouver une zone défilable ; aucune capture, journalisation ou transmission de cette structure au fournisseur IA. Pas de clics arbitraires, achats, saisie de mots de passe ou envois automatiques.

Ces fonctions donnent un accès sensible à certaines capacités du téléphone. Elles ne donnent pas à ANGE les droits de tout faire dans toutes les applications. Toutes restent révocables dans les réglages Android. Le traitement des conversations en ligne est une option séparée, avec ses propres informations et consentement.
