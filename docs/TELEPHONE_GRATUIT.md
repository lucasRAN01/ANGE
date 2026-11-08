# ANGE sur Galaxy A53 5G — sans PC ni API payante

Ce guide décrit l’installation du modèle introduite en 2.0.1-phone. La version **2.1.0-bot-actions** conserve cette installation et ajoute le [bot 3D et les commandes Android](BOT_3D_ET_ACTIONS.md) ; ses nouvelles fonctions et limites remplacent les anciennes mentions d’absence totale de contrôle Android dans ce document.

Cette variante vise le Samsung SM-A536B/DS, 8 Go de RAM. Un petit modèle local est intégré au fonctionnement de l’application, mais son fichier est installé séparément. La vitesse et la chauffe doivent être mesurées sur le vrai téléphone : l’émulateur ne les prédit pas.

## Mise en route

1. Installer l’APK `ANGE-Mobile-Phone-Free-debug.apk` par-dessus la précédente version. Il s’agit d’un APK de débogage signé pour essais personnels. Ne désinstallez l’ancienne version qu’en cas de conflit de signature : la désinstallation efface les réglages et le modèle.
2. Ouvrir ANGE → Compagne (ou Chat) → **IA téléphone**.
3. Appuyer sur **Télécharger · 347 Mo**, confirmer, rester dans l’application et attendre la vérification. Privilégier le Wi-Fi. En quittant l’application, le transfert est annulé ; un nouvel essai recommence depuis le début. Prévoir au moins 500 Mo libres pour le transfert seul ; garder plutôt 1,5 Go libres pour l’application, le modèle et les éventuels caches du moteur.
4. Écrire une question courte et appuyer sur **Envoyer**. Fermer les applications lourdes si la mémoire manque. Le premier chargement peut prendre du temps. **Arrêter** interrompt la demande ; aucune bascule vers le cloud n’est effectuée.
5. Après installation du modèle, activer le mode avion et essayer une autre question écrite. Le texte doit fonctionner sans Internet. Le modèle reste dans le stockage privé de l’application.

Autre méthode : télécharger le [fichier exact du modèle](https://huggingface.co/litert-community/Qwen3-0.6B-int4/resolve/56621f0115eba5781051ef9714c06e16e6d40c0b/qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm), le copier sur le téléphone puis choisir **Importer**. Le sélecteur peut afficher tous les fichiers, mais seuls la taille et le SHA-256 attendus sont acceptés. Une archive ZIP, un modèle GGUF ou un autre fichier LiteRT-LM est refusé. Aucun compte Hugging Face n’est nécessaire pour ce fichier public.

**Options → Supprimer** retire le fichier du modèle et son marqueur de validation. Le moteur peut laisser un cache de poids préparés dans le cache Android (environ 331 Mo observés dans l’émulateur). Pour récupérer aussi cet espace : paramètres Android → Applications → ANGE → Stockage → **Vider le cache**. Ne pas choisir « Effacer les données » si vous souhaitez garder vos réglages et le modèle installé. Après import, la copie d’origine dans Téléchargements reste également un fichier distinct que vous pouvez supprimer volontairement.

## Voix et personnage

Dans **Options**, choisir FR, une voix locale si disponible et **Tester la salutation et la voix**. **Réglages vocaux du téléphone** ouvre les réglages Android pour installer les données vocales françaises. Le libellé et les moteurs proposés dépendent de Samsung et des services installés.

**Parler** demande l’accès au microphone à votre appui. La reconnaissance sur l’appareil est utilisée lorsqu’Android l’annonce disponible. Cela ne garantit pas la présence d’un modèle français hors ligne : le test en mode avion le vérifie. Si elle manque, la saisie écrite reste utilisable. Les options voix réseau et reconnaissance réseau sont désactivées par défaut, ne valent que pour la session et peuvent envoyer respectivement texte ou audio au service vocal installé. Les laisser désactivées pour l’usage strictement local.

L’avatar garde ses huit poses, clignements et respiration. Les mouvements de bouche suivent approximativement la synthèse vocale ; ce n’est ni une animation humaine 3D ni une synchronisation phonème par phonème.

## Gratuité et confidentialité

- Aucun abonnement, clé fournisseur, Supabase, serveur PC ou API d’IA payante n’est nécessaire au mode **IA téléphone**. Les modes Android sont Simple, IA téléphone et PC local. Une ancienne sélection cloud est remplacée par IA téléphone ; le contrôleur refuse le mode cloud même s’il est réinjecté dans les préférences.
- Seul le téléchargement volontaire contacte Hugging Face et ses serveurs de fichiers. Aucune conversation n’y est envoyée. La connexion Internet, les données mobiles, le stockage et l’électricité dépendent toujours de votre équipement et de votre forfait.
- Le modèle ne reçoit ni outils ni accès aux applications. L’historique est limité à la session et n’est pas sauvegardé par ce mode ; la réponse voit au plus quatre messages précédents raccourcis. Les autres écrans de mémoire et leurs exemples restent distincts.
- Le moteur est libéré au retour en arrière-plan ou après environ une minute d’inactivité. Une initialisation native déjà lancée peut prendre du temps avant de s’arrêter. Pas d’écoute permanente, de réveil vocal ou de service de génération en arrière-plan.
- La version web/PC conserve son connecteur cloud facultatif historique. Ne pas le configurer si vous souhaitez conserver un usage sans API payante ; il n’est pas utilisé par ce mode Android.

## Limites par rapport à Siri

Le modèle 0.6B est petit : erreurs, réponses incomplètes, français parfois maladroit et mémoire de conversation courte sont possibles. Réponses plafonnées à 160 jetons, CPU limité à deux threads, contexte moteur de 1024 jetons. Pas de recherche Internet, appels, SMS, lancement d’applications, rappels système, vision ou contrôle du téléphone par l’IA. Le mode **Simple** sait notamment donner heure/date ; le modèle génératif n’a pas d’outil lui donnant automatiquement l’heure réelle.

La RAM totale de 8 Go ne garantit pas que suffisamment de mémoire sera disponible à chaque lancement. Le programme vérifie au moins 1,2 Gio disponibles avant de charger le moteur ; Android peut toutefois arrêter une application sous pression mémoire. Les performances, la stabilité prolongée, l’autonomie et la voix française sur votre A53 nécessitent vos essais.

## Composants et traçabilité

- Moteur : [Google LiteRT-LM Android](https://developers.google.com/edge/litert-lm/android), version **0.16.0**, backend CPU. Le convertisseur du modèle documente sa compatibilité avec cette version ; une montée de version exige un nouvel essai des modèles et du format de conversation.
- Modèle : [litert-community/Qwen3-0.6B-int4](https://huggingface.co/litert-community/Qwen3-0.6B-int4), dérivé de [Qwen/Qwen3-0.6B](https://huggingface.co/Qwen/Qwen3-0.6B), licence Apache 2.0 déclarée par ces dépôts. Pas de poids dans Git, le ZIP source ou l’APK.
- Révision figée : `56621f0115eba5781051ef9714c06e16e6d40c0b`.
- Fichier : `qwen3_0.6b_nothink_q4_block32_ekv1280.litertlm`, **347 251 840 octets**.
- SHA-256 : `2df6821ec12702dafd33915e7a1a1adc7c4b053f3672fd9555dfaf3a114c4139`.

Les résultats de compilation et d’essai propres à cette version sont consignés dans `ETAT_PHONE.md`. Le test instrumental facultatif `PhoneInferenceSmokeTest` exige le modèle déjà provisionné ; il ne télécharge rien et n’est pas un test automatiquement exécuté par la CI sans appareil.
