# Étude d’animation de la mini-compagne ANGE

## Direction visuelle et interface

Photo de référence fournie par l’utilisateur : cheveux noirs bouclés, mèches autour du visage, boucles dorées, robe blanche fleurie à manches bouffantes. L’atlas généré reprend ces repères avec des proportions de mini-personnage adulte stylisé. La photo originale n’est pas incluse dans le dépôt ou l’APK. L’illustration est une interprétation, pas une reconstruction biométrique exacte.

Interface : fond bleu nuit uni, texte blanc, accents menthe et lavande, halo doux derrière le personnage, quatre actions rapides. Les paramètres techniques et les clés sont regroupés dans Réglages. Les anciennes vues sont conservées pour référence. La fonction de chaque animation doit être compréhensible sans attribuer de conscience ou d’émotion réelle au personnage.

## Atlas livré et états

Fichier : `android/app/src/main/res/drawable-nodpi/ange_photo_atlas.png`. Grille 4 × 3, fond transparent, douze cellules. Les coordonnées sont calculées par fractions de la largeur/hauteur réelle, sans supposer des tailles de cellule entières.

| Indices | État | Déclenchement |
| --- | --- | --- |
| 0 / 1 | Repos / clignement | Au repos, clignement court environ toutes les 5,1 secondes |
| 2 / 3 | Bouche ouverte / arrondie | État de lecture TTS et compteur de progression vocale |
| 4 | Écoute | Reconnaissance vocale effectivement démarrée |
| 5 | Réflexion ou problème | Calcul en cours ou action nécessitant une vérification |
| 6 / 7 | Salutation / accueil | Toucher le personnage, pendant une courte durée |
| 8 / 9 | Marche à gauche | Déplacement vers la gauche |
| 10 / 11 | Marche à droite | Déplacement vers la droite |

L’état est calculé par `PhotoMotion`. La priorité est déplacement, salutation, puis état du contrôleur. Les callbacks périmés de reconnaissance et génération sont invalidés par identifiants/compteur de génération. L’animation ne décide jamais d’une action Android.

## Mouvement et cycle de vie

`PhotoCompanionView` dessine l’atlas avec un filtre de texture et une légère oscillation verticale. Rafraîchissement environ 12,5 fois par seconde ; alternance des pas toutes les 220 ms. Les préférences d’animations Android et l’option d’ANGE sont respectées. Les mises à jour s’arrêtent si la vue est détachée, masquée ou inactive. L’atlas décodé est partagé en mémoire ; pas de nouvelle image générée ou téléchargée à chaque mouvement.

Dans l’application : glissement limité à la scène du personnage. En superposition : `WindowManager.TYPE_APPLICATION_OVERLAY`, petite fenêtre transparente, déplacement borné à l’écran, retour vers un bord en 350 ms, balade facultative entre les bords en 1,6 s environ toutes les 22 secondes au repos. Une notification persistante et × permettent de fermer le service. Le verrouillage interrompt le microphone et masque la fenêtre.

Le déplacement de la fenêtre ne signifie pas qu’ANGE explore ou comprend les applications. La navigation système facultative est un service distinct avec des commandes explicites et limitées. Aucun accès à un écran ou aux contacts n’est donné au modèle génératif par cette animation.

## Parole : niveau réellement implémenté

La voix est celle du moteur de synthèse Android sélectionné. Les états de lecture déclenchent deux formes de bouche, avec une alternance rythmique. Il n’y a ni clone vocal de la personne photographiée, ni analyse phonétique précise, ni capture de mouvement facial. Les changements de pose peuvent être visibles : ce sont des images discrètes, pas une interpolation de squelette.

## Pour une animation humaine 3D complète

Le travail restant demande un modèle 3D préparé spécifiquement : géométrie et retopologie, textures peau/cheveux/robe, squelette articulé, poids de déformation, expressions du visage, visèmes et animations de marche/arrêt/salutation/écoute. Une photographie seule ne fournit ni les faces cachées, ni les articulations ou textures nécessaires.

Une évolution pourrait utiliser un modèle glTF/GLB avec squelette, animations et blend shapes, puis un moteur de rendu Android. Il faudrait traiter les transitions entre états, l’ancrage des pieds, le mouvement secondaire des cheveux et de la robe, les collisions aux bords, l’orientation du regard et la synchronisation voix/visèmes. Ces éléments ne sont pas fournis par l’atlas actuel. Le robot procédural OpenGL déjà présent prouve le rendu 3D, mais ne remplace pas ce travail d’art et de rigging pour un personnage humain.

## Validation attendue sur Galaxy A53 8 Go

Vérifier l’animation avec/sans économie d’énergie, la chauffe après 10–20 minutes, l’impact mémoire avec le modèle local chargé, la persistance du service Samsung, la rotation d’écran, le clavier, le verrouillage et les écrans sécurisés. Mesurer la parole réellement audible et le français reconnu. Les essais sur émulateur ne permettent pas d’annoncer l’autonomie, les performances GPU ou la dictée du téléphone réel comme validées.

Les tests de politique parcourent les douze indices et l’arrêt des animations. Les essais Android vérifient l’atlas et le cycle du rendu. Les preuves de vérification de cette version décrivent séparément ce qui a été exécuté.

Création visuelle par l’outil intégré image_gen : [prompt complet et dimensions réellement obtenues](design/photo-avatar-prompt.txt), [atlas utilisé dans l’application](../android/app/src/main/res/drawable-nodpi/ange_photo_atlas.png).
