# ANGE

ADVANCED NEURAL GUARDIAN ENGINE

ANGE Companion ajoute une compagne animée et parlante au design `ANGE_2_0_Design_Fidele`, avec une application Android native et un service PC local/cloud. Le projet suit A.docx comme cahier des charges de l’écosystème. **Il ne réalise pas encore l’intégralité de cet assistant.**

**ANGE 2.2 : base Jarvis réellement intégrée.** La révision `15f3f87e0bfd3fe68ec38deafbfeab22636c3993` du dépôt demandé par A.docx est conservée sous `third_party/jarvis/`, avec ses 309 fichiers et sa licence MIT. Le service PC charge ses modules originaux de mémoire et de texte vocal. Android utilise leurs ports Kotlin pour une mémoire persistante et les filtres de lecture. Voir [l’audit](docs/audit/JARVIS_AUDIT.md), [la table de provenance et le guide](docs/JARVIS_INTEGRATION.md) et [les mentions de licence](THIRD_PARTY_NOTICES.md). Les autres intégrations Jarvis ne sont pas automatiquement activées.

**Bot 3D, voix et actions Android.** Un robot en vraie 3D locale s’ajoute au portrait illustré. Des commandes explicites permettent d’ouvrir les applications, de préparer appels/SMS/événements, de demander alarmes/minuteurs et de régler le volume multimédia. La discussion vocale peut enchaîner les tours tant qu’ANGE reste au premier plan. Voir [le guide bot 3D et commandes](docs/BOT_3D_ET_ACTIONS.md).

**IA directement sur le téléphone.** Le modèle libre Qwen3 0.6B INT4 est téléchargé ou importé une fois (347 Mo). Les réponses sont ensuite calculées sur le CPU du téléphone, sans PC, Supabase, compte fournisseur ni API payante. Voir [le guide Galaxy A53](docs/TELEPHONE_GRATUIT.md). Aucun téléchargement de modèle n’est lancé sans appui et confirmation. Une mise à jour de l’APK conserve le modèle déjà installé.

## Compagne mobile

- Même personnage féminin, huit poses illustrées, clignements, respiration, écoute attentive, salutation et mouvements de bouche pendant la lecture vocale.
- Saisie texte, historique limité à la session, synthèse vocale Android, sélection des voix réellement présentes, interruption et arrêt à la mise en arrière-plan.
- Microphone demandé seulement après appui sur Parler. Reconnaissance sur l’appareil privilégiée ; reconnaissance réseau uniquement après autorisation explicite de session.
- Trois modes Android : **Simple** (heure/date/salutation, sans IA générative), **IA téléphone** (Qwen3 embarqué après installation du modèle), **PC local** (Ollama facultatif). Aucun mode cloud ni appel IA payant depuis cet APK ; une ancienne préférence cloud est remplacée par IA téléphone. Le connecteur cloud demeure dans la version web/PC et exige sa propre configuration et confirmation.
- Jeton de connexion chiffré au repos avec Android Keystore ; clés du fournisseur cloud uniquement sur le PC.

Le choix **Bot 3D** affiche un robot procédural OpenGL avec articulations, éclairage, rotation au toucher et expressions. **Portrait 2D** conserve les huit poses de la compagne illustrée. Dans les deux cas, la bouche suit approximativement les événements de lecture, sans correspondance phonème par phonème. La voix et les langues disponibles dépendent du moteur Android installé. Le malgache n’est pas annoncé comme validé. Les commandes Android sont analysées séparément ; les réponses du modèle ne sont jamais exécutées.

## Essayer le personnage sur Windows

Décompresser le livrable puis lancer `OUVRIR_COMPAGNE.cmd`. L’aperçu local permet les poses, la salutation, la lecture si une voix locale est disponible et les réponses limitées heure/date. Le bouton Compagne animée de la planche et son téléphone donnent aussi accès à cet aperçu. La version HTML reste distincte de l’application Android native.

Pour utiliser les réponses IA dans le navigateur, démarrer ANGE Core puis ouvrir **http://127.0.0.1:8765/ui/companion/index.html** sur le PC. Ouvrir le fichier HTML directement ne configure pas la liaison au service. Le service sert la page et l’API sur la même origine ; aucun accès CORS global n’est ouvert.

## Démarrer ANGE Core sur le PC

Installer Python 3.11 ou 3.12. Dans `core`, exécuter :

```powershell
python -m venv .venv
.\.venv\Scripts\python.exe -m pip install -r requirements.txt
$env:ANGE_OLLAMA_MODEL = "nom-exact-du-modele-deja-installe"
# Facultatif pour le cloud : définir OPENAI_API_KEY et ANGE_CLOUD_MODEL sur ce PC.
.\.venv\Scripts\python.exe main.py
```

`Start-Core.ps1` peut créer l’environnement et lancer le service. Le serveur écoute **127.0.0.1** par défaut. Le jeton d’accès est créé dans `core/.local/access-token.txt` ; collez son contenu dans les réglages de la compagne. Ce jeton n’est pas la clé du fournisseur IA. Il n’est pas inclus dans Git ou dans le ZIP livré. Changer ou supprimer ce fichier, avec arrêt du service, renouvelle les accès qui l’utilisent.

**Local PC** exige Ollama en cours d’exécution avec un modèle déjà installé et adapté à votre matériel. Aucun modèle n’est téléchargé automatiquement, imposé comme optimal ou déclaré testé sur Ryzen 5 / 8 Go. Une seule génération est acceptée simultanément et le contexte est limité.

**Cloud** exige un compte fournisseur, une clé et un identifiant de modèle valide configurés par variables d’environnement. Le connecteur fourni utilise l’API Responses d’OpenAI avec `store: false`. Cela ne signifie pas qu’aucune donnée n’est traitée ou conservée par le fournisseur selon ses propres conditions. Aucun appel cloud réel n’a lieu sans configuration et confirmation ; aucun modèle ou crédit payant n’a été acheté par ce projet.

L’historique de conversation n’est pas enregistré par le service. Le contexte envoyé au modèle est limité aux 20 derniers messages transmis par le client. L’API `/v1/notes` fournit séparément des notes SQLite ajoutées volontairement : création, lecture, modification et suppression. Elles ne sont pas mémorisées silencieusement par le modèle. La base SQLite du PC n’est pas chiffrée : ne pas la considérer comme un coffre pour informations sensibles.

## Connecter le téléphone au PC

Le téléphone doit être sur le même réseau privé. **Une liaison LAN exige HTTPS** ; le lancement du serveur sur une adresse non locale est refusé sans certificat et clé. Aucun port ne doit être exposé directement sur Internet.

```powershell
.\.venv\Scripts\python.exe -m pip install -r requirements-tls.txt
.\.venv\Scripts\python.exe setup_tls.py --name mon-pc.local --ip 192.168.1.20
.\.venv\Scripts\python.exe main.py --host 192.168.1.20 --cert .local/server-cert.pem --key .local/server-key.pem
```

Remplacer les valeurs par le nom et l’adresse privés du PC. Installer **seulement** le certificat public `core/.local/ange-ca.crt` comme autorité utilisateur sur votre téléphone personnel, via ses paramètres de sécurité. Conserver les clés `.pem` sur le PC. La configuration de l’application fait confiance aux autorités système et utilisateur ; aucune vérification TLS n’est désactivée. Le certificat serveur expire après 90 jours.

Dans les réglages de la compagne Android, saisir `https://192.168.1.20:8765` ou le nom correspondant au certificat, puis le jeton. Enregistrer et tester la connexion. Une règle de pare-feu privée peut être nécessaire ; le projet ne modifie pas automatiquement le pare-feu et n’active pas de redirection de ports.

Ce mécanisme est une **authentification manuelle par jeton partagé**, pas encore l’appairage multi-appareils avec QR code, identités individuelles et révocation par appareil prévu à terme. Le débogage Android autorise HTTP seulement vers l’hôte d’émulateur `10.0.2.2` ; la version de production l’interdit.

## Android et vérifications

Ouvrir `android` dans Android Studio avec JDK 17 et SDK / Build Tools 35. Gradle 8.11.1, AGP 8.10.1, Kotlin/Compose compiler 2.2.21 et LiteRT-LM 0.16.0 sont les versions du projet. La cible reste API 35, le minimum API 26. Le moteur embarqué contient les bibliothèques ARM64 et x86_64. Le Galaxy A53 physique n’est pas accessible pour une mesure de performances.

```powershell
.\android\gradlew.bat -p android :app:assembleDebug :app:testDebugUnitTest
```

La page Compagne est affichée au démarrage. Accueil, Mobile, Chat et Voix donnent accès à cette interaction. Les autres modules conservent les fonctions et limites explicites du design initial. Les préférences et l’historique de la nouvelle compagne ne sont pas confondus avec les exemples fictifs des panneaux de design.

Les résultats exacts de compilation, tests et essais figurent dans `docs/ETAT_COMPANION.md`. La CI fournie vérifie l’API et construit un APK de débogage ; sa présence ne constitue pas une preuve qu’un workflow GitHub a déjà réussi.

## Périmètre restant

Les actions Android couvrent les intentions publiques des applications : lancement, numérotation, brouillons SMS/Agenda, demandes Horloge et volume multimédia. Le contrôle automatique des boutons internes d’autres applications, le contrôle Windows, la vision, Home Assistant, la lecture des notifications, le réveil vocal permanent, la mémoire chiffrée et la synchronisation complète restent à développer. La mémoire structurée Jarvis 2.2 est persistante mais privée et non chiffrée en supplément, avec ajouts explicites. Les mémoires PC et Android et l’ancienne API SQLite de notes restent distinctes. Aucun code arbitraire ou commande système n’est exécuté par le modèle.

Les versions jusqu’à 2.1 ne réutilisaient pas Jarvis. L’audit et l’intégration commencent avec 2.2, sans modifier cet historique. Les sources d’origine sont séparées des adaptateurs ANGE et leurs empreintes sont vérifiables avec `python tools/verify_jarvis.py`. Les fichiers synchronisés du cahier des charges et des références restent en lecture seule. Les illustrations et le bot 3D restent des réalisations ANGE, avec leur provenance documentée.
