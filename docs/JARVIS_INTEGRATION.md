# Base Jarvis réellement intégrée à ANGE 2.2

## Ce qui change

Les versions ANGE jusqu’à 2.1 étaient une implémentation distincte. À partir de **2.2.0-jarvis-base**, le projet inclut une copie de référence vérifiable du dépôt demandé par A.docx et utilise réellement certains de ses composants. L’historique précédent n’est pas réécrit et l’ensemble de Jarvis n’est pas présenté comme déjà adapté ou activé. L’import est un snapshot traçable ; il ne transforme pas le dépôt ANGE en fork GitHub avec tout l’historique amont.

Origine : [sosoj92/jarvis-assistant-vocal](https://github.com/sosoj92/jarvis-assistant-vocal), révision **15f3f87e0bfd3fe68ec38deafbfeab22636c3993**, licence **MIT, Copyright (c) 2026 sosoj92**. Les 309 fichiers d’origine sont conservés dans `third_party/jarvis/`, séparés des extensions ANGE. Leurs empreintes sont dans `docs/audit/jarvis-inventory.json` ; `python tools/verify_jarvis.py` vérifie leur contenu et la licence présente dans l’APK. L’audit initial couvre 216 fichiers Python analysés syntaxiquement, sans erreur avec Python 3.12.14, et une lecture ciblée documentée ; ce n’est pas une certification complète de sécurité.

## Table de provenance et d’activation

| Composant d’origine | Intégration effective | Adaptations / limites |
|---|---|---|
| `core/memoire.py` | Le service PC charge **le fichier original inchangé** via `core/jarvis_adapter.py`. Ses fonctions de chargement, sauvegarde atomique et récupération sont exécutées. | Chemin et verrou isolés par service, stockage à côté de la base ANGE dans `jarvis-memory/`. Une seule instance/worker par fichier recommandée ; pas de verrou interprocessus ajouté. |
| `tools/memoire.py` | Les opérations remember/recall/forget sont adaptées dans l’adaptateur PC et dans `JarvisMemoryStore.kt`. | Ajouts uniquement volontaires, validation des types et tailles, limite de 500 souvenirs, confirmation d’oubli côté interfaces. L’auto-mémorisation suggérée par Jarvis n’est pas activée. |
| `core/util.py` | Fichier Python original chargé et utilisé sur les réponses du service ; port `JarvisVoice.kt` utilisé pour le nettoyage vocal et la normalisation des commandes Android. | Port Kotlin de la normalisation NFKD et des motifs d’introduction. Les phrases naturelles restent imparfaitement comprises. |
| `core/confidentialite.py` | Fichier Python original produit `speech_reply`, utilisé par la compagne web ; port Kotlin appliqué avant la lecture Android. | Texte visible conservé, option « Masquer les infos sensibles à la lecture » sur Android, activée par défaut. Filtre heuristique : pas une détection exhaustive des secrets. |
| Schéma mémoire `preferences/people/projects/facts` | Persistance sur PC et téléphone, recherche sans accents, oubli avec copie de récupération, effacement complet avec copies. | Les mémoires PC et Android restent **indépendantes**, sans synchronisation automatique et sans injection automatique au modèle IA. |
| Registre, permissions, providers, voix Python, outils et autres intégrations Jarvis | Sources conservées pour poursuivre la transformation. | Aucun auto-import de tous les outils, aucune clé ou intégration activée par cette livraison. Whisper/Piper, wake word, Twilio, Gmail, Hue, OBS, Hermes et contrôle Windows complet ne sont pas annoncés fonctionnels dans ANGE. |
| Android Compose, LiteRT-LM, bot 3D et commandes Android 2.1 | Extensions ANGE maintenues. | Ce code reste distinct : il ne provient pas rétroactivement de Jarvis. Le moteur Python complet ne tourne pas sur Android. |

## Utilisation sur le téléphone

1. Installer `ANGE-Mobile-Jarvis-Base-debug.apk` comme mise à jour. Le modèle local déjà installé est conservé ; aucun téléchargement de modèle supplémentaire n’est demandé.
2. Ouvrir **Mémoire** dans la navigation. Choisir Faits, Préférences, Proches ou Projets ; saisir une étiquette si nécessaire et le souvenir, puis **Mémoriser localement**.
3. Fermer et rouvrir ANGE : le souvenir est conservé. La recherche ignore les accents et la casse.
4. **Oublier ce sujet…** retire les éléments correspondants après confirmation et garde une copie locale de récupération. **Effacer toute la mémoire et sa copie…** supprime aussi cette copie après confirmation.
5. Dans **Compagne → Options**, le filtre de confidentialité vocale est visible. **Base Jarvis · origine et licence MIT** affiche la provenance et le texte intégral de la licence, sans connexion.

La mémoire est dans les fichiers privés de l’application, **sans chiffrement supplémentaire**. Elle ne doit pas être utilisée comme coffre à mots de passe. Le port Android emploie `AtomicFile` ; un fichier illisible est conservé et bloque les nouvelles écritures jusqu’à un effacement explicite, plutôt que d’être remplacé silencieusement. Une désinstallation efface les données. Les anciennes notes temporaires de l’écran de design ne sont pas importées ni annoncées comme synchronisées.

Le bot 3D et la conversation restent accessibles comme en 2.1. Les voix, la reconnaissance française, la chauffe et la fluidité sur le Galaxy A53 doivent être vérifiées sur ce téléphone réel. L’ajout de Jarvis ne transforme pas le petit modèle 0.6B en modèle cloud plus puissant.

## Utilisation côté PC

Le service conserve son lancement et son authentification. Après mise à jour des sources, le redémarrer volontairement pour charger ANGE Core 0.4.0. Les identifiants, certificats et bases existants ne sont pas écrasés. Aucun installateur Jarvis ne doit être lancé pour utiliser ces seuls modules : leurs dépendances sont celles de la bibliothèque standard Python.

Nouvelles routes authentifiées :

- `GET /v1/memory?query=...` : consultation et recherche, sans envoi à un fournisseur.
- `POST /v1/memory` : JSON `{"category":"preferences","key":"boisson","text":"Aime le café"}` ; catégories `preferences`, `people`, `projects`, `facts`.
- `POST /v1/memory/forget` : JSON `{"subject":"café","confirmed":true}` ; une copie de récupération reste sur le PC.
- `POST /v1/memory/erase` : JSON `{"confirmed":true}` ; retire la mémoire et ses copies de récupération de ce stockage.

Sans `confirmed: true`, les routes d’oubli et d’effacement refusent l’opération. L’ancienne API SQLite `/v1/notes` reste distincte. Le filtre des réponses vocales ne modifie pas le texte visible contenant une adresse ou un numéro : la lecture peut utiliser sa version masquée.

## Vérification et portée

Les tests portent sur les modules réellement activés, leurs adaptations et les garanties de non-envoi automatique. Les résultats exacts sont dans `ETAT_JARVIS_BASE.md`. Les fonctionnalités d’autres modules ne sont pas considérées validées par la simple importation de leur source. Les dépendances et services optionnels conservent leurs licences et coûts propres ; aucun compte payant ou appel IA payant n’a été configuré pour cette intégration.
