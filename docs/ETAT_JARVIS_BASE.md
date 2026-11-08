# Vérification de la base Jarvis — ANGE 2.2

Version Android **2.2.0-jarvis-base**, code 4. ANGE Core **0.4.0**. Révision Jarvis intégrée : **15f3f87e0bfd3fe68ec38deafbfeab22636c3993**. Audit et intégration du 8 novembre 2026 ; voir [JARVIS_INTEGRATION.md](JARVIS_INTEGRATION.md) pour le périmètre exact.

## Résultats exécutés

| Vérification | Résultat |
|---|---|
| Inventaire de l’origine | 309 fichiers suivis, 216 fichiers Python analysés syntaxiquement, zéro erreur avec Python 3.12.14 |
| Fidélité du snapshot | Les 309 empreintes sont vérifiées par `tools/verify_jarvis.py`, avec tolérance des seules fins de ligne Windows |
| Tests originaux Jarvis retenus | **14 réussis**, exécutés depuis le snapshot livré : mémoire, confidentialité et hallucinations de transcription |
| Tests du service ANGE | **20 réussis**, avec mémoire explicite, persistance, sauvegardes, limites, authentification et absence de souvenirs dans le prompt du modèle |
| Tests JVM Android | **24 réussis**, dont les nouveaux tests de normalisation, nettoyage et confidentialité vocale |
| Compilation | APK principal et APK instrumental construits avec JDK 17 / Gradle 8.11.1 / AGP 8.10.1 / Kotlin 2.2.21 / SDK et Build Tools 35 |
| Essais instrumentés Android | **8 réussis**, sur émulateur API 35 x86_64 dédié, 3072 Mio et rendu GPU hôte |
| Compagne navigateur | Tests réussis ; événements de synthèse vocale simulés, sans validation d’un son entendu |
| Licence APK | Texte MIT et révision Jarvis effectivement lus depuis les assets installés par le test instrumental |
| Signature APK | Vérifiée et identique aux versions précédentes livrées localement, permettant la mise à jour avec conservation des données |

Les huit essais instrumentés couvrent : intentions Android et permissions, volume réel restauré après test, confirmations et arrêt de discussion en arrière-plan, mémoire persistante et recherche sans accents, oubli avec copie puis effacement, conservation d’un fichier corrompu, licence et filtre exécutés sur Android, inférence locale avec le modèle figé, rendu 3D et son arrêt/reprise. Les cas de mémoire utilisent un répertoire de cache isolé ; ils n’effacent jamais les souvenirs personnels de l’application cible.

Un essai manuel dans l’interface a ajouté « Souvenir demo Jarvis persistant », puis arrêté complètement et relancé l’application en mode avion. Le souvenir a été relu dans l’écran Mémoire. La capture `verification/jarvis-memory.png` et la hiérarchie XML associée montrent cet exemple de test, sans donnée personnelle.

Les filtres Kotlin ont été corrigés pour employer des classes Unicode compatibles avec le moteur de regex Android ICU ; un test JVM seul ne suffisait pas à valider cette compatibilité. La version livrée a ensuite passé les tests sur émulateur.

## Ce qui n’est pas prouvé par ces essais

- Le fonctionnement de l’ensemble des intégrations de Jarvis : seules les fonctions indiquées dans la table de provenance sont activées et testées dans ANGE.
- L’installation complète du projet Python >= 3.13 et de toutes ses dépendances. Les installateurs d’origine n’ont pas été lancés ; les modules PC utilisés ici ne demandent pas de SDK cloud/audio supplémentaire.
- Un audit exhaustif de sécurité, des vulnérabilités de toute la chaîne de dépendances ou un test d’intrusion. L’inventaire est exhaustif, la lecture manuelle est ciblée.
- Le son réellement entendu, la reconnaissance d’une voix française, les performances, la chauffe et l’autonomie sur le Galaxy A53 physique. Ces tests doivent être effectués sur ce téléphone avec ses services vocaux.
- Une synchronisation PC/téléphone ou une mémoire chiffrée supplémentaire. Les fichiers sont privés/localement stockés et lisibles par le propriétaire de l’environnement ; ils ne sont pas un coffre à secrets.
- Une amélioration de la qualité du modèle 0.6B. Ses réponses peuvent rester maladroites ou hors sujet. La mémoire n’est pas injectée automatiquement dans ses instructions.

Les sources Jarvis d’origine et leur licence sont conservées, les adaptateurs ANGE et ports Android restent séparés. Le bot 3D, le moteur LiteRT-LM et les commandes Android des versions précédentes ne sont pas attribués à Jarvis. Les données et clés du serveur personnel ainsi que les références synchronisées sous `sources/` n’ont pas été écrasées ou modifiées. Aucun appel IA payant ni communication sortante réelle n’a été effectué par les essais.
