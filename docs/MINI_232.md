# ANGE Android 2.3.2 mini — installation et essais

Installer **ANGE-Mobile-Mini-232-debug.apk** par-dessus la version précédente, **sans désinstaller**. Code de version 8, même signature de développement. Le modèle et les préférences existants restent dans les données de l’application. Fermer l’ancienne mini-compagne avant la mise à jour, puis ouvrir ANGE.

## Mini-compagne unique

Le personnage flottant mesure **116 dp de haut**, contre 186 dp (environ 38 % de réduction). Barre sombre de 180 dp avec quatre zones tactiles :

- Crayon : ouvrir ANGE.
- Bulle de message : ouvrir la discussion écrite et son clavier, même depuis un autre écran.
- Onde audio : parler après autorisation du microphone.
- Flèche : masquer les commandes ou fermer la mini-compagne. Toucher le personnage restaure les commandes ; la notification permet aussi de l’arrêter.

Plus de texte « Touchez Parler… », ni de bulle vide au repos. Les vraies réponses et erreurs restent visibles. Le robot OpenGL et l’ancien portrait alternatif ont été retirés. Le même mini-personnage apparaît dans les quatre espaces ; lorsque l’affichage flottant est actif, le personnage intégré est masqué pour éviter les doublons.

Le personnage est une illustration issue de la photo de référence, animée par **douze poses 2D** (repos, écoute, parole approximative, salutation, déplacement). Il ne s’agit pas d’un humain 3D articulé ni d’une simulation physique de marche. La photo originale n’est pas distribuée.

## Présence et voix lors d’une ouverture d’application

Dans ANGE → Réglages, autoriser l’affichage au-dessus des applications puis activer la mini-compagne. Le service visible et l’écran principal partagent maintenant **une seule session et un seul moteur de lecture** : quitter ANGE pour ouvrir une application ne doit plus fermer la voix de la mini-compagne.

La préférence de présence est mémorisée ; une fermeture volontaire reste respectée. La mini-compagne ne s’arrête plus avec la tâche principale. Android peut néanmoins tuer un processus, restreindre la batterie ou masquer les fenêtres sur un écran protégé. Une restauration système peut réafficher le personnage sans démarrer le microphone ; dans ce cas, l’icône voix revient dans ANGE pour réarmer la session. Il n’y a pas de démarrage après redémarrage du téléphone, de mot de réveil permanent ni de contournement des écrans protégés. Le verrouillage masque le personnage et arrête le micro. Une autre application qui prend la sortie audio peut interrompre la lecture.

Sur le Galaxy A53, si nécessaire : Paramètres Android → Applications → ANGE → Batterie → choisir l’autorisation d’activité appropriée et vérifier « Apparaître par-dessus ». Tester ensuite un retour sur l’accueil, l’ouverture de Notes et celle de l’appareil photo. Le service de microphone reste visible dans les indicateurs Android ; utiliser Arrêter pour terminer la discussion.

## Noms d’applications

« Ouvre Notes » peut correspondre à **Samsung Notes** ; accents, espaces, articles et pluriels simples sont normalisés. Un nom exact est prioritaire. Plusieurs applications correspondantes provoquent un choix ; une ressemblance approximative n’entraîne jamais une ouverture silencieuse. Dire le nom complet ou sélectionner l’application permet de trancher.

La liste est actualisée au retour dans ANGE, sur demande dans Actions et une fois lorsqu’une application n’est pas trouvée. Seules les applications lançables visibles dans le profil Android courant sont proposées ; un profil professionnel, une application masquée ou sans activité de lancement peut rester absent. La reconnaissance vocale dépend toujours du moteur Android : cette correction ne peut pas garantir une transcription correcte de tous les noms.

## Commandes natives ajoutées

| À dire ou écrire | Résultat et accord nécessaire |
| --- | --- |
| Allume la lampe / Éteins la lampe | Commande du flash par Android. Permission Caméra demandée à l’usage, sans photo ni ouverture d’une caméra. Peut échouer si le flash est occupé ou absent. |
| Verrouille le téléphone | Première fois : écran Android pour autoriser ANGE comme administrateur **limité à force-lock**. Après activation, redire la commande. Aucun droit d’effacement ni de changement de mot de passe. Un code/PIN peut être nécessaire au déverrouillage. Sans verrouillage configuré, Android peut seulement mettre l’écran en veille. |
| Capture d’écran | Fenêtre de consentement Android **à chaque capture**. Une seule image PNG enregistrée dans **Images / ANGE** (Pictures/ANGE), sans envoi au modèle. La mini-compagne est temporairement masquée, puis restaurée. Un contenu protégé peut être noir ou non capturable. Avant Android 10, le fichier reste dans le dossier Images propre à ANGE. |
| Active / Désactive le Wi-Fi | Ouvre le panneau Wi-Fi ; vous actionnez l’interrupteur. |
| Active / Désactive le Bluetooth | Ouvre les réglages Bluetooth ; vous actionnez l’interrupteur. |
| Active / Désactive les données mobiles | Ouvre le panneau Internet ; vous validez la connexion mobile. |

Retirer le droit de verrouillage dans ANGE → Réglages, bouton « Retirer le droit de verrouillage d’ANGE », ou dans les administrateurs d’appareil Android. Ce droit est facultatif et peut devoir être retiré avant une désinstallation.

Android limite les changements directs des radios pour les applications ordinaires. ANGE n’annonce pas qu’un interrupteur a changé lorsqu’elle a seulement ouvert le panneau. Elle ne peut pas exécuter « toutes les tâches » de toutes les applications. Retour/Récentes/défilement interapplications restent indisponibles dans cette version standard sans service d’accessibilité. Aucun root ni contournement n’est ajouté.

## Vérification sur votre téléphone

1. Ouvrir ANGE après la mise à jour ; vérifier qu’il ne reste qu’un mini-personnage.
2. Activer l’affichage flottant. Déplacer mini-ANGE ; tester les quatre icônes, masquer les commandes, toucher le personnage pour les retrouver.
3. Depuis Réglages, toucher la bulle de message : la discussion et le clavier doivent s’ouvrir.
4. Dire « Ouvre Notes », puis le nom de deux autres applications installées. Vérifier le choix si plusieurs noms correspondent. Utiliser Actions → Actualiser les applications si nécessaire.
5. Faire lire une réponse puis ouvrir une application : vérifier la continuité vocale sur le Galaxy A53. Tester l’accueil et la caméra ; les appels ou autres sources audio peuvent prendre priorité.
6. Tester la lampe après permission Caméra, puis l’éteindre. Tester la capture en annulant d’abord, puis en acceptant ; vérifier le PNG dans Images/ANGE.
7. Si souhaité, autoriser le verrouillage puis redemander de verrouiller. Vérifier auparavant que vous connaissez le PIN du téléphone. Retirer ensuite le droit pour tester sa révocation.
8. Tester Wi-Fi, Bluetooth et données mobiles : chaque commande doit afficher son panneau et demander votre geste final.
9. Fermer mini-ANGE avec sa flèche → Fermer. Revenir dans ANGE : elle doit rester fermée jusqu’à une nouvelle activation.

## Limites de validation

Compilation et tests automatisés sont documentés dans les preuves `docs/verification/mini232-*`. Les essais utilisent un émulateur dédié ; ils ne certifient pas la voix française, le flash matériel, la gestion de batterie Samsung, l’acceptation Play Protect ou la continuité sonore sur votre Galaxy A53. Garder Play Protect actif. Aucun fournisseur IA payant n’a été appelé pour ces vérifications.

Les actions sont exécutées par des fonctions Android déterministes, avant le modèle. La discussion locale Qwen3 0.6B garde ses limites ; les nouvelles actions ne transforment pas ce petit modèle en ChatGPT. Gemini facultatif nécessite votre configuration et votre consentement. Le projet reste un assistant partiel ; toutes les fonctions du cahier des charges A.docx ne sont pas réalisées.

Références Android : [capture et consentement](https://developer.android.com/media/grow/media-projection), [verrouillage DevicePolicyManager](https://developer.android.com/reference/android/app/admin/DevicePolicyManager#lockNow()), [lampe CameraManager](https://developer.android.com/reference/android/hardware/camera2/CameraManager#setTorchMode(java.lang.String,%20boolean)), [fenêtres protégées](https://developer.android.com/security/fraud-prevention/activities).
