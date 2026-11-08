# Couverture de la livraison fidèle

Document historique du prototype de design initial. Les fonctions ajoutées ensuite sont décrites dans `ETAT_BOT_ACTIONS.md` et `JARVIS_INTEGRATION.md` ; les anciennes mentions d’absence totale d’audio ou de code Jarvis ne décrivent pas la version 2.2 actuelle.

| Écran | Windows | Android natif | Limite |
|---|---|---|---|
| 01 Démarrage | Logo, noyau lumineux, progression visuelle, entrée | Noyau illustré et entrée | Aucun chargement de modules IA |
| 02 Accueil | Recomposition de la fenêtre PC de référence, cinq cartes, activité et statut | Accueil illustré, microphone et six tuiles | Statuts de démonstration, aucun capteur système |
| 03 Mobile | Aperçu compact, six raccourcis et téléphone interactif dans la planche | Destination Mobile avec même accueil natif | Pas de liaison PC |
| 04 Voix | Six états manuels, séquence, anneaux et onde décorative | Six états manuels avec noyau partagé | Pas de capture audio, STT, TTS ou réaction au volume |
| 05 Chat | Saisie, historique de session, copie, choix langue/moteur | Saisie et historique de session, moteur | Réponse fixe et explicitement fictive, pas de génération à interrompre |
| 06 Cerveau | États techniques et indicateurs non mesurés | États techniques et moteur choisi | Pas de modèle, télémétrie ou pensées internes |
| 07 Mémoire | Notes CRUD, import/export JSON et confirmations | Notes CRUD de session | Pas de base durable, chiffrement ou synchronisation ; pas d’export Android |
| 08 Liaison | Parcours d’appairage fictif et statuts | Même parcours fictif | Aucun jeton réel, serveur ou synchronisation |
| 09 Contrôles | Confirmation et annulation de demandes simulées | Dialogues de confirmation natifs | Aucune commande système ou domotique exécutée |
| 10 Paramètres | Langue préférée, animation, confidentialité | Langue et notification simulée | Pas de traduction complète, voix installée ou notification réelle |

## Reproduction graphique

La planche de 1536 × 1024 reprend les coordonnées de la fenêtre PC, la barre latérale, les cartes, les panneaux, le téléphone et la compagne. La vue application agrandie privilégie la lecture et rend les dix destinations visibles. Sur petit écran, elle remplace automatiquement la planche pour éviter de réduire tout le visuel à des dimensions illisibles.

La compagne au casque cyan, les anneaux/circuits, la Terre et le cerveau sont des illustrations détaillées générées à partir de la référence. Les fichiers PNG sources et WebP optimisés sont fournis. Le logo et les icônes sont vectoriels ou dérivés de ces vecteurs. Android affiche ces ressources locales dans des composants natifs Compose, sans WebView.

Les textes, boutons, menus, cartes, réglages et notes sont de vrais éléments HTML/Compose. Aucun bouton n’est incrusté dans une image de fond. Le téléphone de la planche est un aperçu HTML interactif ; il ne constitue pas une capture Android exécutée. Les six petits panneaux contiennent des exemples ou des contrôles UI ; le chat rapide et les interrupteurs sont manipulables. Les notes d’exemple de la planche sont distinctes des notes personnelles saisies dans le module Mémoire.

## Différences conservées et périmètre restant

Les illustrations ont été recréées : le visage, les détails des anneaux, les nuages et les particules peuvent varier. La typographie est celle disponible dans le navigateur. Les chiffres de performance et les connexions réussies de l’affiche ont été remplacés par des libellés honnêtes sur le prototype. La livraison ne revendique donc pas une copie identique pixel à pixel ni un assistant complet.

Vision ouvre une explication de l’intégration manquante. Les graphiques sont des illustrations, les notes du dock sont des exemples et les dates sont fictives. La langue choisie est une préférence UI, sans traduction intégrale.

Les phases backend décrites dans A.docx restent à construire séparément : audit du dépôt Jarvis et des licences, API et sécurité, persistance durable, moteurs IA/audio, liaison PC/mobile, permissions et contrôle système, vision, traductions, benchmarks matériels, compilation et essais sur appareils Android 16 et Windows 11. Aucun code du dépôt Jarvis n’est intégré ou déclaré fonctionnel dans cette livraison de design.
