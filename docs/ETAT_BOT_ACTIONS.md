# État de la livraison Android 2.1.0-bot-actions

Finalisation du 8 novembre 2026, après les premiers essais du 7 novembre. Le guide d’utilisation est [BOT_3D_ET_ACTIONS.md](BOT_3D_ET_ACTIONS.md). Les essais 2.0.1 antérieurs sont conservés séparément dans `ETAT_PHONE.md`.

## Ce qui est implémenté

- Robot procédural réellement rendu en 3D par OpenGL ES : géométries, matériaux éclairés, tête et bras articulés, respiration, clignement, états d’écoute/réflexion/lecture, bouche animée approximativement avec la voix, rotation horizontale au geste, salutation au toucher. Choix entre ce robot et le portrait 2D précédent.
- Commandes Android explicites, traitées avant toute génération IA : lancement d’une application visible, réglages, caméra, volume multimédia, demande d’alarme/minuteur, préparation d’appel, SMS et événement Agenda, ouverture de recherche et d’itinéraire. Les réponses du modèle ne sont pas exécutées et les commandes sont exclues de son contexte conversationnel suivant.
- Confirmation dans ANGE pour les appels préparés, SMS, alarmes, minuteurs et événements. Appel et envoi de SMS nécessitent ensuite votre action dans leur application ; Agenda reste un brouillon à enregistrer. Pas de permissions CALL_PHONE ou SEND_SMS.
- Discussion vocale au premier plan : réouverture du microphone après la fin de la synthèse vocale, arrêt volontaire, arrêt sur erreur et à la mise en arrière-plan. Aucun mot de réveil permanent ni microphone en arrière-plan.
- Même modèle Qwen3 0.6B, stockage privé et mode gratuit sur téléphone. Une mise à jour conserve le modèle déjà installé. Aucun fournisseur IA payant n’est appelé par cet APK.

## Vérifications

La version porte `versionCode=3`, `versionName=2.1.0-bot-actions`, minimum API 26 et cible API 35. Compilation avec JDK 17, Gradle 8.11.1, AGP 8.10.1 et Kotlin/Compose 2.2.21. La signature de débogage est vérifiée et identique à celle des APK précédemment livrés localement.

**21 tests JVM Android réussis** : les 11 tests précédents et 10 nouveaux tests pour les noms d’applications exacts et ambigus, les propos qui ne sont pas des commandes, les numéros sans lecture de contacts, les brouillons SMS avec accents, les durées et horaires, les variantes orales simples, le volume borné, les réglages, les adresses Web et les événements.

**5 tests instrumentés sur émulateur Android API 35 x86_64, 3072 Mio** :

1. Énumération Launcher et modification réelle du volume multimédia, puis restauration du volume initial.
2. Construction des intentions de communication : ACTION_DIAL, ACTION_SENDTO et corps SMS ; absence des permissions d’appel direct/envoi SMS ; paramètres de l’alarme et interface Horloge demandée.
3. Demandes de confirmation, annulation, activation du mode discussion avec un moteur TTS prêt, puis arrêt en arrière-plan et suppression de la demande en attente.
4. Vraie inférence CPU avec le modèle figé, contrôle de son SHA-256, rejet d’un import corrompu et préservation du modèle existant. Ce test s’exécute sans réseau.
5. Présence de la surface 3D native, plusieurs images réellement rendues, absence d’erreur OpenGL, rotation et état de lecture, arrêt du rendu quand l’activité passe en arrière-plan, reprise au retour.

Des essais manuels ont aussi ouvert les **réglages Android** et **l’Horloge** depuis l’interface ANGE. Une commande d’alarme a affiché la confirmation `07:30`, puis a créé dans l’Horloge de l’émulateur une alarme portant le nom ANGE. Cette seule alarme de test a ensuite été supprimée. Les deux alarmes préexistantes ont été conservées. Les preuves XML et le journal manuel sont dans `verification/bot-*`.

L’aperçu final [bot-3d.png](verification/bot-3d.png) est une capture réelle en **1080 × 1920** avec le rendu accéléré par le GPU du PC, après inspection visuelle. Une première tentative en affichage logiciel a provoqué un blocage de System UI dans l’émulateur ; elle n’est pas utilisée comme aperçu. Les tests du rendu et de son arrêt/reprise ont réussi, mais ces essais ne constituent pas une mesure de performances du GPU Samsung. Une courte démonstration vidéo muette montre aussi la salutation et la rotation dans l’application.

## Limites de ces preuves

L’émulateur ne remplace pas le Galaxy A53 physique. Les applications Samsung, les restrictions de profil, la stabilité prolongée, le débit graphique, la chauffe, la batterie et les performances de l’inférence restent à mesurer sur le téléphone. Les tests d’intentions ne prouvent pas un appel ou un SMS réellement envoyé, ni l’enregistrement d’un événement Agenda : aucune communication sortante n’a été faite lors de ces essais.

L’émulateur est lancé sans sortie audio : un moteur TTS prêt et ses événements ne prouvent pas un son entendu. La reconnaissance d’une vraie voix française, les accents et l’enchaînement complet micro → réponse → lecture → micro doivent être testés sur votre appareil avec les packs adéquats. Les erreurs et refus gardent une solution de saisie écrite. La permission de microphone reste demandée au geste.

Le modèle 0.6B peut produire du français maladroit ou des réponses hors sujet, et les temps de réponse observés sur émulateur ne prédisent pas ceux de l’A53. Le moteur de commandes déterministe n’utilise pas sa sortie pour exécuter des actions. Le robot est un compagnon stylisé à géométrie procédurale ; il n’est pas un avatar humain photoréaliste et sa bouche n’est pas synchronisée phonème par phonème.

Le contrôle arbitraire des boutons internes des autres applications, la lecture des notifications, le contrôle Windows, l’envoi autonome de messages, le réveil vocal permanent et l’ensemble des capacités de Siri restent hors du périmètre de cette version. Les fichiers du cahier des charges sous `sources/` et les secrets/certificats du serveur personnel n’ont pas été modifiés.
