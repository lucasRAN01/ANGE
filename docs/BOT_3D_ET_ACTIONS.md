# ANGE 2.1 — bot 3D et actions Android

Cette version complète le mode gratuit sur téléphone avec un robot 3D local, une discussion vocale au premier plan et des commandes Android. Elle vise notamment le Galaxy A53 5G SM-A536B/DS, 8 Go. Les limites de l’inférence locale décrites dans `TELEPHONE_GRATUIT.md` restent valables.

## Installer et parler

1. Installer `ANGE-Mobile-Bot3D-Actions-debug.apk` en mise à jour de la version précédente. L’application conserve le même identifiant et le modèle local déjà installé. Une désinstallation efface les données ; elle n’est pas nécessaire si vous utilisiez l’APK signé livré précédemment.
2. Ouvrir **Compagne** ou **Chat**, sélectionner **Bot 3D** et choisir **IA téléphone**. Si le modèle Qwen3 est déjà présent, rien à télécharger de nouveau. Sinon, installer les 347 Mo comme dans le guide téléphone. Les commandes Android fonctionnent aussi en mode **Simple**, sans modèle.
3. Dans **Options**, choisir FR, une voix locale française et tester la salutation. Utiliser **Réglages vocaux du téléphone** pour installer ses données si besoin.
4. Appuyer sur **Parler**, autoriser le microphone et dire une question ou une commande. La réponse écrite reste disponible si la voix n’est pas prête.
5. Pour enchaîner les tours de parole, choisir **Discussion vocale · démarrer**. Le micro se rouvre après la fin de la lecture, avec une courte pause. **Arrêter**, une erreur vocale, une commande Android ou le passage en arrière-plan terminent ce mode. Il faut le démarrer de nouveau volontairement.

Pas de réveil permanent « Hey ANGE », ni d’écoute écran verrouillé. La disponibilité de la reconnaissance française locale dépend des services et packs réellement installés sur le téléphone. Pour rester strictement hors ligne, laisser désactivées les options voix réseau et reconnaissance réseau, puis tester en mode avion. Une panne ou une langue indisponible n’entraîne pas de bascule réseau automatique.

## Utiliser le bot 3D

Le robot est construit avec de vrais volumes, des articulations, des normales et un éclairage OpenGL ES 2.0. Il est rendu à la résolution de sa surface Android, sans vidéo préenregistrée ni image simulant la 3D. Son style est celui d’un petit robot métallique blanc et cyan, avec visière et logo orbital ANGE. Il n’est pas un humain photoréaliste.

Il flotte, bouge la tête, cligne des yeux, montre l’écoute, anime sa bouche pendant la synthèse vocale et salue de la main au toucher. Glisser horizontalement le fait tourner. Les mouvements de bouche suivent approximativement l’état de lecture et ses événements : ils ne correspondent pas exactement aux phonèmes.

**Portrait 2D** permet de retrouver la compagne illustrée précédente. Le réglage **Animations du personnage** et la réduction des animations Android figent les mouvements ; le rendu 3D s’arrête en arrière-plan. La cadence est plafonnée à environ 30 images/s pour limiter la charge ; le débit réel et la consommation doivent être mesurés sur l’A53. Un échec graphique détecté active le portrait 2D.

## Commandes disponibles

Le bouton **Actions** affiche les exemples et la liste des applications visibles. Dans l’onglet **Applications**, un appui ouvre l’application sélectionnée. Les noms à dicter doivent correspondre à cette liste ; deux applications de même nom demandent un choix dans la liste.

| Dire ou écrire | Ce qu’ANGE fait réellement |
|---|---|
| `Ouvre WhatsApp` ou `Lance YouTube` | Lance l’application si elle est installée et visible dans la liste. |
| `Ouvre appareil photo` | Ouvre l’application caméra ; vous prenez la photo. |
| `Ouvre paramètres`, `Ouvre Wi-Fi`, `Ouvre Bluetooth` | Affiche les réglages correspondants ; vous changez les options. |
| `Volume à 40 %` | Règle le volume multimédia, sans modifier le volume des appels ou des alarmes. |
| `Appelle +261340000000` | Après confirmation, ouvre le numéro dans Téléphone. Vous appuyez sur Appeler. |
| `Prépare un SMS au +261340000000 : À bientôt` | Après confirmation, ouvre un brouillon SMS avec numéro et texte. Vous appuyez sur Envoyer. |
| `Alarme à 7h30` | Après confirmation, transmet heure et minutes à l’application Horloge. Vérifier le résultat dans cette application. |
| `Mets une alarme à sept heures trente` | Même action avec cette variante orale. Les horaires relatifs ou toutes les formulations françaises ne sont pas interprétés. |
| `Minuteur de 5 minutes` ou `Mets un minuteur de cinq minutes` | Après confirmation, transmet la durée à l’Horloge, entre une seconde et 24 heures. |
| `Ajoute un événement : Réunion équipe` | Après confirmation, ouvre un brouillon Agenda ; vous choisissez la date et enregistrez. |
| `Itinéraire vers Antananarivo` | Ouvre une proposition d’itinéraire dans Maps ou le navigateur. La navigation n’est pas démarrée automatiquement. |
| `Cherche météo Antananarivo` | Ouvre la recherche Web dans le navigateur. Le modèle local ne reçoit pas automatiquement les résultats. |
| `Ouvre https://example.org` | Ouvre cette adresse HTTP/HTTPS dans le navigateur. |

Les exemples de numéro sont fictifs : remplacez-les par le bon numéro avant de confirmer. Les noms de contacts ne sont pas lus ; `Appelle Maman` demande un numéro et ne devine pas le destinataire. Les actions absentes, les applications non installées et les formats non reconnus peuvent être refusés. Les profils professionnels, applications masquées et restrictions parentales ou administrateur peuvent limiter la liste ou le lancement.

## Portée et confidentialité

- Les commandes sont reconnues dans le texte **fourni ou dicté par l’utilisateur**, avant le modèle. Une réponse de l’IA n’est jamais interprétée comme une commande. Les échanges de commande sont séparés du contexte envoyé ensuite au modèle de conversation.
- Pas de service d’accessibilité, root, accès global aux fichiers, lecture des SMS, lecture des contacts, appel direct ou permission d’envoyer des SMS. Les applications externes conservent leurs écrans de validation. Le résultat affiché par ANGE signale la demande d’ouverture ; il ne certifie pas qu’un message a été envoyé ou qu’un rendez-vous a été enregistré.
- L’application ajoute deux permissions normales : réglage audio et demande d’alarme. Le microphone reste demandé à l’usage. Les applications visibles sont découvertes avec la catégorie Launcher, sans `QUERY_ALL_PACKAGES`.
- Le moteur IA et le bot ne nécessitent ni Supabase, PC, compte fournisseur, abonnement ou API payante. Les applications ouvertes peuvent toutefois utiliser Internet, un compte ou un abonnement existant ; les appels, SMS et données suivent votre forfait habituel. Le Web et Maps ne sont pas présentés comme hors ligne.
- Le contrôle automatique des boutons internes de WhatsApp, la lecture des notifications, l’envoi autonome de messages, les appels sans confirmation, les rappels système propres à ANGE, la vision et le contrôle de Windows ne sont pas réalisés ici. Ce n’est pas encore l’ensemble des fonctions de Siri.

Les résultats précis de compilation, de tests et de vérification visuelle sont dans `ETAT_BOT_ACTIONS.md`. Les tests sur émulateur ne remplacent pas les essais de microphone, de son, de vitesse et de chauffe sur votre Samsung.

## Références d’implémentation

Cette intégration s’appuie sur les API Android officielles : [intentions courantes](https://developer.android.com/guide/components/intents-common), [visibilité des applications](https://developer.android.com/training/package-visibility/use-cases), [AlarmClock](https://developer.android.com/reference/android/provider/AlarmClock), [AudioManager](https://developer.android.com/reference/android/media/AudioManager) et [OpenGL ES](https://developer.android.com/develop/ui/views/graphics/opengl). Le robot et ses géométries sont créés dans le code du projet ; aucun service de génération d’avatar n’est requis.
