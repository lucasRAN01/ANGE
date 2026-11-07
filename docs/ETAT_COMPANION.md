# État de la livraison Companion

Cette révision poursuit le design fidèle, et ajoute une première chaîne fonctionnelle de compagne mobile et de conversation PC. Elle ne termine pas toutes les phases de A.docx.

## Réalisé

La compagne dispose de huit poses illustrées, d’un clignement, d’une respiration légère, de poses d’écoute, de réflexion et de salutation. Les formes de bouche alternent pendant l’état de lecture signalé par le moteur vocal. L’animation et les communications sont arrêtées quand l’application passe en arrière-plan. L’image n’est pas un modèle 3D et les lèvres ne suivent pas exactement chaque phonème.

Android utilise les API SpeechRecognizer et TextToSpeech réelles. Le microphone est soumis à une permission après action de l’utilisateur. La reconnaissance locale est choisie si disponible ; la reconnaissance réseau demande une autorisation de session distincte. Les voix listées sont celles réellement retournées par Android. Une voix réseau n’est pas utilisée sans autorisation. Les erreurs de service, d’autorisation et de langue sont affichées, avec saisie texte conservée.

ANGE Core est une API FastAPI authentifiée. Le mode local appelle Ollama, le mode cloud appelle l’API Responses via une clé sur le PC. Les modèles sont configurables, sans téléchargement automatique. Un message cloud et jusqu’à 20 messages de contexte exigent une confirmation. Le local ne bascule jamais silencieusement vers le cloud. Les conversations ne sont pas conservées par le service ; des notes SQLite distinctes peuvent être ajoutées volontairement par l’API.

Le serveur écoute en boucle locale par défaut. Un lancement sur une adresse LAN exige des certificats TLS. Le téléphone vérifie la chaîne de certificats et peut utiliser une autorité locale installée volontairement par l’utilisateur. Le jeton Android est sauvegardé avec AES-GCM et une clé Android Keystore. Les clés IA ne sont jamais placées dans l’APK ou le JavaScript. Le jeton PC et les notes SQLite restent des fichiers locaux non chiffrés : aucune protection de coffre chiffré n’est revendiquée côté PC.

## Vérifications effectuées

- **Android : compilation de l’APK de débogage réussie**, JDK 17, Gradle 8.9, AGP 8.7.3, SDK et Build Tools 35. L’archive officielle Gradle a été vérifiée avec son SHA-256. Le Wrapper est livré avec cette somme dans ses propriétés.
- **Android : 7 tests JVM réussis**, pour les adresses HTTPS et l’exception debug d’émulateur, le refus des adresses avec identifiants ou chemin, les poses de bouche, l’arrêt des animations, le clignement, les états et les réponses limitées.
- **Émulateur Android API 35 : installation et ouverture réussies** de l’APK, activité principale active et capture réelle du personnage. L’émulateur est dédié à ce test, distinct des appareils virtuels préexistants. Il ne constitue pas un essai sur Android 16.
- **API PC : 13 tests réussis**, couvrant authentification, routage local/cloud, consentement avant cloud, filtrage du texte de sortie, validation, taille des requêtes, notes persistantes et absence de repli cloud après échec local.
- **Navigateur : tests de compagne réussis**, heure/date en mode limité, historique, salutation, variation des poses, arrêt, refus de dictée sans autorisation, annulation cloud, réduction des mouvements et largeur 360 px. Le moteur vocal y est simulé pour les tests de cycle de vie : cela ne valide pas un son réellement entendu.
- Les dix écrans du design Windows et la planche adaptée à la compagne ont également passé leurs tests de navigation et d’affichage.

Les résultats machine et captures sont dans `docs/verification`. Les tests d’API utilisent des réponses simulées des fournisseurs et ne prouvent pas la qualité d’un modèle installé. Aucun appel cloud payant ou installation d’un modèle lourd n’a été effectué.

## Essais restant nécessaires

Voix réellement audible sur téléphone, disponibilité d’une voix française féminine et malgache, reconnaissance des accents, interruption de lecture sur plusieurs moteurs, rotation, restauration, Android 16 et consommation mesurée sur le matériel Ryzen 5 / 8 Go. La connexion HTTPS téléphone-PC et une génération avec le modèle choisi par l’utilisateur doivent aussi être validées dans son réseau.

Le contrôle système, les rappels/notifications réels, Home Assistant, la vision, l’agenda, le wake word continu et l’appairage par appareil restent non implémentés. Le mode Mobile limité fournit seulement des réponses déterministes pour l’heure, la date et quelques salutations. Il n’est pas présenté comme une IA générative locale.

La CI fournie compile Android et lance les tests. Son état réel doit être lu dans GitHub après le push ; cette documentation ne prétend pas qu’une exécution distante a déjà réussi.

## Références techniques

Les intégrations suivent les documentations primaires [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer), [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech), [UtteranceProgressListener](https://developer.android.com/reference/android/speech/tts/UtteranceProgressListener), [Ollama Chat](https://docs.ollama.com/api/chat), [génération de texte OpenAI](https://developers.openai.com/api/docs/guides/text) et [sécurité FastAPI](https://fastapi.tiangolo.com/tutorial/security/). Aucun clone de voix réelle n’est utilisé.
