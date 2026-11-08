# Audit initial de la base Jarvis — 8 novembre 2026

## Référence et méthode

Dépôt demandé par A.docx : https://github.com/sosoj92/jarvis-assistant-vocal. Révision examinée et figée : **15f3f87e0bfd3fe68ec38deafbfeab22636c3993**. L’inventaire exhaustif des fichiers suivis, de leurs tailles, empreintes et imports statiques figure dans `jarvis-inventory.json`.

L’analyse comprend l’inventaire de l’arbre complet, l’analyse syntaxique des fichiers Python avec l’interpréteur disponible, et une lecture ciblée de l’architecture, des fournisseurs IA, de la mémoire, du registre, des confirmations, du serveur, des utilitaires, des outils Windows, des scripts d’installation et des tests associés. Elle ne constitue ni une revue manuelle ligne par ligne de chaque intégration, ni un audit de sécurité exhaustif ou un test d’intrusion. Les fonctionnalités annoncées par le README sont distinguées des composants effectivement testés dans ANGE.

## Architecture observée

- `jarvis14.py` orchestre la boucle vocale ; `core/` contient configuration, IA, mémoire, permissions, routage, voix et serveur.
- `tools/` contient les fonctions enregistrées par décorateur. Le registre découvre les modules et décrit les outils aux fournisseurs IA. Les appels, navigateur, courriels, contrôle PC et autres intégrations disposent de conditions et confirmations propres.
- `core/llm.py` adapte plusieurs fournisseurs à une interface commune. L’import peut injecter le magasin de certificats système ; ce module n’est donc pas chargé indirectement dans ANGE pour réutiliser seulement ses utilitaires.
- `core/memoire.py` utilise un JSON structuré `preferences`, `people`, `projects`, `facts`, un verrou réentrant, une écriture temporaire avec remplacement atomique et des copies de récupération.
- `core/util.py` fournit la normalisation sans accents et le nettoyage des introductions artificielles de réponses vocales. `core/confidentialite.py` masque plusieurs motifs sensibles avant lecture ; ce filtre est heuristique et incomplet.
- Le registre distingue des niveaux N1/N2/N3. `core/confirmation_vocale.py` traite explicitement négation, hésitation et accords conditionnels. Ces contrôles n’équivalent pas à une isolation du système d’exploitation.
- `desktop_agent/`, les satellites, les scripts et le panneau web sont des composants séparés. La présence de code pour une intégration ne prouve pas qu’elle est configurée ou testée sur notre matériel.

## Licence et origine

Le fichier `LICENSE` déclare **MIT, Copyright (c) 2026 sosoj92**. La copie originale, les mentions et la révision sont conservées avec le code importé. Les ports Android conservent également ce crédit et une copie de la licence dans l’APK. La licence du dépôt ne remplace pas celles de ses dépendances, services ou modèles ; aucune conclusion globale sur toutes leurs conditions n’est revendiquée.

L’import de référence reste séparé des extensions ANGE. Une table de provenance indique les fichiers utilisés au fonctionnement, les ports adaptés et les composants simplement conservés pour les prochaines étapes. Ajouter le snapshot ne signifie pas que l’ensemble de Jarvis s’exécute dans l’application Android.

## Dépendances et installation

`pyproject.toml` demande **Python >= 3.13** et de nombreuses dépendances, dont des SDK cloud, audio, navigateur, CUDA et intégrations optionnelles. `uv.lock` fige une résolution ; les contraintes `>=` du manifeste seules ne garantissent pas une installation reproductible sans ce verrou. Les versions réellement déployées restent à vérifier pour chaque intégration.

`INSTALLER_JARVIS.bat` peut installer uv et Git avec winget, synchroniser Python et les paquets, installer Chromium et lancer l’assistant de configuration. Il n’a pas été exécuté. Les scripts de démarrage, mise à jour et diagnostic sont lus comme code de référence, pas lancés automatiquement. Aucune clé n’est configurée et aucun appel cloud ou service payant n’est activé par cet audit.

La pile Python/Windows complète ne peut pas être importée telle quelle dans Kotlin/Android. Les modules standard-library retenus sont chargés séparément côté PC ; leurs adaptations Android restent natives. Le modèle LiteRT-LM, les API vocales Android et le bot 3D existants restent des extensions ANGE, avec leurs propres provenances.

## Risques et décisions d’intégration

| Constat vérifié dans le code | Décision pour ANGE |
|---|---|
| Découverte automatique des outils et nombreux effets possibles | Pas d’auto-import de `tools/`, pas d’exécution d’outil produit par le modèle. |
| Outils Windows, extinction, navigateur et communications | Conservés comme référence, non activés par cette première intégration. |
| Serveur et tunnels publics optionnels | Aucun tunnel ou port Internet ouvert ; les protections HTTPS/authentification d’ANGE restent en place. |
| Mémoire JSON lisible et copies de récupération | Stockage privé/local, écriture explicite seulement, limites de taille, confirmation avant oubli et possibilité d’effacement avec copies. Pas de promesse de coffre chiffré. |
| Le module mémoire initial peut injecter les souvenirs dans la consigne système | Pas d’injection automatique des souvenirs dans l’IA ; la consultation demeure explicite. |
| Certains outils mémorisent automatiquement les préférences | Ce comportement n’est pas activé : seul l’utilisateur ajoute des souvenirs. |
| Filtre de confidentialité heuristique | Réutilisé pour la lecture, sans le présenter comme une protection parfaite contre tous les secrets. |
| Comptes, matériels et services optionnels absents | Gmail, Twilio, Hue, OBS, satellites, Hermes, vision et wake word ne sont pas déclarés validés. |

## Composants retenus

1. **Mémoire structurée** : réutilisation réelle du module Python original et adaptation des opérations de mémorisation/recherche/oubli ; port du schéma et des opérations explicites sur Android avec persistance atomique.
2. **Utilitaires de réponse vocale** : réutilisation du nettoyage et de la normalisation, avec port Android et tests de correspondance.
3. **Confidentialité vocale** : réutilisation du filtre Python et adaptation Kotlin avec option visible pour la lecture.
4. **Reste du dépôt** : copie de référence figée, distincte du code activé. Les prochaines intégrations doivent être testées et documentées séparément ; la présence du code ne vaut pas livraison de l’assistant complet.

Les résultats exécutés et les correspondances précises seront consignés dans `../JARVIS_INTEGRATION.md` et dans les journaux de vérification. Cet audit corrige le choix initial d’une implémentation indépendante, sans transformer rétroactivement l’historique des versions déjà livrées.
