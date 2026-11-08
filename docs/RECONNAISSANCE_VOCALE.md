# Reconnaissance vocale : codes 12 et 13 — Android 2.2.1

Le code **12** d’Android signifie que le moteur de reconnaissance choisi ne prend pas en charge la langue demandée. Le code **13** signifie que la langue est reconnue par le moteur mais que son modèle est actuellement indisponible, par exemple non téléchargé. Une disponibilité globale du moteur local ne garantit pas la disponibilité du français.

L’ancien message « Aucune réponse IA inventée » indiquait qu’aucune question transcrite n’avait été envoyée au modèle. Ce n’était pas une réponse de l’IA. Il ne constitue pas non plus une garantie contre les erreurs des futures réponses générées.

## Installer et essayer

1. Installer `ANGE-Mobile-Voice-Fix-debug.apk` **par-dessus** ANGE, sans désinstaller ni effacer ses données. Il porte le code de version 5, nom `2.2.1-voice-fix`, et la même signature de développement que nos APK locaux précédents. Une mise à jour compatible conserve le modèle et les réglages.
2. Ouvrir **Compagne / Chat → Options → FR**. Le code envoyé au moteur est maintenant `fr-FR`, au lieu du code générique `fr`.
3. Pour rester hors ligne, laisser **Reconnaissance vocale réseau autorisée** désactivée. Appuyer sur **Réglages de reconnaissance du téléphone**. Choisir le français dans le service proposé et installer son modèle hors ligne si ce service le permet. Le bouton **Réglages vocaux du téléphone** concerne séparément la *lecture* des réponses et ne suffit pas à configurer la transcription du microphone.
4. Selon la version Samsung, ces réglages se trouvent aussi dans **Paramètres → Gestion globale → Liste des claviers et clavier par défaut → Saisie vocale Google**, ou dans les paramètres de saisie vocale du clavier Samsung. Chercher « saisie vocale » dans les paramètres si les intitulés diffèrent. Installer **Français** dans la reconnaissance hors connexion si ce menu existe. Un modèle destiné au clavier n’est pas nécessairement exposé au moteur local utilisé par les applications.
5. Revenir dans ANGE, appuyer sur **Parler**, autoriser le microphone et dire « Quelle heure est-il ? ». Cette commande est traitée sans modèle génératif. Vérifier ensuite une question écrite en mode **IA téléphone**, pour tester l’IA séparément.
6. Si le code 12 persiste, essayer avec le Wi-Fi et **Reconnaissance vocale réseau autorisée** activée dans ANGE, puis appuyer de nouveau sur **Parler**. Le moteur local reste prioritaire ; s’il refuse la langue avec le code 12 ou 13, ANGE essaie **une seule fois** le service Android par défaut. Ce service peut envoyer votre audio à son fournisseur. Cette autorisation est réinitialisée au prochain lancement du contrôleur ; elle ne configure aucune IA cloud payante et ne change pas le mode de réponse choisi.
7. Pour vérifier le fonctionnement strictement hors ligne, désactiver à nouveau l’option réseau, puis couper le Wi-Fi et les données et refaire le test. Une reconnaissance qui ne fonctionne qu’avec le service réseau n’est pas déclarée hors ligne.

Mettre à jour les services vocaux installés depuis le Play Store peut également aider ; cela ne garantit pas qu’un modèle de français hors ligne soit proposé sur ce téléphone. Aucun service vocal français embarqué indépendant de Samsung/Google n’a été ajouté à cet APK. Si le service refuse toujours le français, la saisie écrite fonctionne et le microphone du clavier peut servir à remplir le champ, selon les capacités et les réglages de confidentialité du clavier.

## Changements techniques et limites

- Tags régionaux `fr-FR`, `en-US`, `mg-MG`. Le tag malgache n’implique pas que le moteur installé le prenne en charge.
- Repli uniquement pour les codes 12/13 du moteur **local**, uniquement avec consentement de session. Pas de repli sur une erreur d’autorisation, un silence, une absence de correspondance ou un service occupé ; pas de boucle sur le service de secours.
- Arrêter, quitter l’application, changer de langue ou révoquer l’autorisation annule l’écoute et invalide le repli en attente.
- Aucun résultat vocal ni réponse IA de remplacement n’est fabriqué quand la reconnaissance échoue. La génération peut toutefois produire des erreurs lorsque de vraies demandes lui sont envoyées.
- Les coûts éventuels de connexion dépendent du forfait ; ce correctif n’ajoute ni abonnement IA ni clé de fournisseur.
- Les essais automatisés vérifient le routage, le consentement, les paramètres Android et la compilation. Ils ne valident pas une vraie dictée française sur le Galaxy A53 : le téléphone de l’utilisateur n’est pas connecté à notre banc de test.

Références : [constantes de SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer#ERROR_LANGUAGE_NOT_SUPPORTED), [réglages de saisie Samsung](https://www.samsung.com/us/support/answer/ANS10001592/).
