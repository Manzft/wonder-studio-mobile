# Wonder Studio Mobile

Editor de contenido de **Wonder Maker** para Android, hecho a medida con
**Kotlin + Jetpack Compose**. Es la versión móvil de Wonder Studio (PC): crea
proyectos con estilos, temas, personajes y objetos, edita sus componentes y el
Wonder Script, y exporta/importa mods con el **mismo formato JSON** que el
Studio de PC y el juego. Objetivo: **ligero y optimizado** (APK debug ~10 MB,
sin motor embebido ni librerías pesadas).

## Funcionalidades

- **Proyectos**: crear/abrir/guardar en una carpeta elegida con el selector del
  sistema (SAF), lista de recientes y validación de `project.json`.
- **Estilos / Temas / Personajes / Objetos**: crear, abrir, duplicar, renombrar,
  reordenar y eliminar.
- **Assets browser**: navega `assets/` del proyecto, previsualiza imágenes y
  reproduce sonidos/música.
- **Lienzo**: previsualiza sprites, animaciones, colliders, areas y raycasts con
  pan, zoom (pinch) y arrastre del componente seleccionado en el eje X/Y.
- **Jerarquía**: agrega, duplica, renombra, elimina y reordena componentes.
- **Inspector**: todas las propiedades por tipo de componente y de la raíz
  (settings `{type, value}`), con selector de assets para los campos de ruta.
- **Animaciones**: administra animaciones de `animated_sprite` (crear, duplicar,
  renombrar, eliminar, reordenar), edita textura/frames/FPS/rango/offset/repeat y
  previsualiza la línea de tiempo (play/pause/stop).
- **Wonder Script**: editor con cabecera `wscript` obligatoria, resaltado de
  sintaxis y autocompletado (builtins, eventos, keywords, códigos de input,
  propiedades y métodos de componente).
- **Mods**: exporta un mod (todo el proyecto o una selección por estilo con sus
  assets referenciados) e importa mods como overlay de sesión.
- **Test**: lanza la app de Wonder Maker por `Intent`, pasándole el proyecto (y
  el mod) como URIs de SAF, y recibe sus logs por UDP (`9174`).

## Requisitos

- JDK 17
- Android SDK (platform `35`, build-tools `35.0.0`)
- `minSdk 26` (Android 8+), `targetSdk 35`

## Compilar

```bash
# Indicá el SDK (o creá local.properties con sdk.dir=/ruta/al/Sdk)
export ANDROID_HOME=/ruta/al/Android/Sdk

./gradlew :app:assembleDebug      # APK de debug en app/build/outputs/apk/debug/
./gradlew :app:assembleRelease    # APK release (R8 + shrink, sin firmar)
```

El `start_bot.sh` de este repo no aplica: es una app Android. Para instalar el
APK: `adb install app/build/outputs/apk/debug/app-debug.apk` o copiarlo al
dispositivo.

## Estructura

| Archivo | Descripción |
|---------|-------------|
| `model/Model.kt` | Modelo y formato JSON (Project/Style/Element/Component/Animation/Setting) + valores por defecto. |
| `data/ProjectRepository.kt` | Lectura/escritura del proyecto en SAF, assets, import/export de mods. |
| `data/Saf.kt` | Utilidades sobre `DocumentFile`. |
| `data/AssetRepository.kt` | Decodificación y caché (`LruCache`) de texturas. |
| `data/AppConfig.kt` | Preferencias: proyectos recientes y paquete del juego. |
| `data/RunLauncher.kt` | Lanza la app de Wonder Maker por `Intent`. |
| `net/UdpLogReceiver.kt` | Escucha los logs UDP (9174) y el handshake del juego. |
| `ui/EditorViewModel.kt` | Estado del editor y operaciones sobre el proyecto. |
| `ui/projects/ProjectsScreen.kt` | Menú de proyectos. |
| `ui/editor/EditorScreen.kt` | Pantalla del editor, panel de proyecto (drawer), export y logs. |
| `ui/editor/EditorPanels.kt` | Jerarquía, inspector, animaciones y assets. |
| `ui/editor/WonderScriptEditor.kt` | Editor de Wonder Script (resaltado + autocompletado). |
| `ui/canvas/SceneCanvas.kt` | Lienzo con gestos y previsualización de componentes. |
| `ui/inspector/PropertySpecs.kt` | Definición declarativa de los campos del inspector. |
| `ui/common/*` | Diálogos, explorador de assets y editor de propiedades. |

## Notas de diseño

- **Mismo formato**: los proyectos son intercambiables con el Studio de PC y el
  juego (`project.json` + `sourcecode/styles/...`).
- **SAF**: los proyectos viven donde el usuario elija; los assets se leen por
  `content://` sin copiarlos, con caché de bitmaps.
- **Optimización**: solo se decodifican texturas con downsample, se cachean por
  ruta, y el lienzo dibuja únicamente los componentes del elemento abierto.
- **Sin dependencias pesadas**: Compose + `kotlinx.serialization` +
  `documentfile`. No se usan motores ni librerías de imagen de terceros.

## Créditos

Fangame hecho por fans, sin ánimo de lucro. Todos los derechos de las
franquicias pertenecen a Nintendo.
