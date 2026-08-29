# Diagnostico Del Modelo

## Modelo utilizado

- Base oficial Vosk: `vosk-model-small-es-0.42`
- Tipo: modelo pequeno en espanol.
- ZIP fuente descargado desde Vosk: `https://alphacephei.com/vosk/models/vosk-model-small-es-0.42.zip`
- Compatibilidad anadida: `graph/words.txt` procedente del modelo oficial `vosk-model-es-0.42`, porque la distribucion pequena oficial carga en Vosk pero no trae ese archivo y Ricardo pidio verificarlo explicitamente.

## Ruta en assets

El modelo completo se incluye en:

`app/src/main/assets/model-es/`

Archivos verificados dentro del APK generado:

- `assets/model-es/am/final.mdl`
- `assets/model-es/conf/mfcc.conf`
- `assets/model-es/conf/model.conf`
- `assets/model-es/graph/Gr.fst`
- `assets/model-es/graph/HCLr.fst`
- `assets/model-es/graph/words.txt`

Nota tecnica: el modelo espanol pequeno oficial `vosk-model-small-es-0.42` trae `graph/Gr.fst` y `graph/HCLr.fst`, pero no trae `graph/words.txt`. Se comprobo con Vosk en local que el modelo pequeno oficial carga correctamente incluso sin `words.txt`; aun asi, la V3 incluye `words.txt` oficial como archivo de compatibilidad para cumplir la verificacion solicitada sin volver al APK de 1,5 GB.

## Ruta interna

En el Galaxy, la app copia el modelo desde assets a almacenamiento privado:

`filesDir/vosk-model-es/`

No usa almacenamiento compartido ni rutas externas hardcoded.

## Versionado

La copia interna incluye:

`filesDir/vosk-model-es/model-version.txt`

Valor actual:

`3`

Si cambia la version incluida en la APK, la app elimina la copia anterior y recopia el modelo completo.

## Flujo de carga

La inicializacion queda protegida por un bloqueo sincronizado en `ModelRepository`.

Flujo aplicado:

1. `PREPARING_MODEL`: muestra `Preparando microfono...`
2. Copia del modelo en un executor de IO.
3. Verificacion de archivos obligatorios.
4. Creacion de `Model`.
5. `READY`: muestra `Escuchando...`
6. Creacion de `Recognizer(model, 16000.0f)`.
7. Creacion de `SpeechService(recognizer, 16000.0f)`.
8. Inicio de escucha.

No se crea `Recognizer` ni `SpeechService` antes de que `Model` exista.

## Logs

Etiqueta de Logcat:

`NOTAS_VOZ`

Incluye:

- inicio de copia;
- fin de copia;
- ruta del modelo;
- archivos obligatorios encontrados;
- duracion de carga;
- inicio de escucha;
- errores completos con stack trace.

## Tiempos y rendimiento

- Tamano del modelo en assets: `65 MB`.
- Tamano APK V3: `53 MB`.
- Carga local de referencia en N95 con Vosk Python:
  - `model_load_s=0.260`
  - `recognizer_create_s=0.054`
  - `python_maxrss_mb=137.8`
- Primer arranque en Android: copia el modelo una vez a `filesDir`.
- Arranques siguientes: no debe recopiar el modelo si `model-version.txt` y los archivos obligatorios estan correctos.
- Si el proceso Android sigue vivo, `ModelRepository` reutiliza la instancia `Model` ya cargada y solo crea un `Recognizer` nuevo por sesion.

## Pruebas realizadas aqui

- `./gradlew --no-daemon assembleDebug`: correcto.
- Verificacion del APK con `unzip -l`:
  - `assets/model-es/am/final.mdl`
  - `assets/model-es/conf/mfcc.conf`
  - `assets/model-es/conf/model.conf`
  - `assets/model-es/graph/Gr.fst`
  - `assets/model-es/graph/HCLr.fst`
  - `assets/model-es/graph/words.txt`
  - solo librerias nativas `lib/arm64-v8a/`
- APK generado vigente: `/home/n95/gDrive/Descargas_OpenClaw/APKs/NotasVozLocal_v0.7_simple_swipes_debug.apk`
- Version Android: `0.7-simple-swipes`
- Cambios de bloqueo/autoguardado:
  - Se retira la notificacion publica de bloqueo de V0.6.
  - Se retira el segundo icono `Grabar nota`.
  - `MainActivity` vuelve a ser el unico lanzador normal.
  - El flujo principal vuelve a ser: abrir app, pulsar `Nueva nota de voz`, captura escucha automaticamente.
  - `CaptureActivity` registra `ACTION_SCREEN_OFF` y llama a guardado automatico.
  - `CaptureActivity` mantiene `FLAG_KEEP_SCREEN_ON` mientras captura para evitar apagado accidental; si el usuario apaga la pantalla, se guarda.
- Cambios de lista:
  - Deslizar hacia la derecha en una nota la elimina directamente.
  - Deslizar hacia la izquierda abre `ACTION_SEND` con `Intent.createChooser`.
  - Tocar una nota sin deslizar abre edicion.
  - El menu `...` conserva copiar, editar y eliminar, y anade compartir.

## Comparativa V2 / V3

| Metrica | V2 | V3 |
|---|---:|---:|
| Tamano APK | 1.5 GB | 53 MB |
| Modelo | `vosk-model-es-0.42` grande | `vosk-model-small-es-0.42` + `words.txt` oficial |
| Arquitecturas nativas | varias del AAR | solo `arm64-v8a` |
| Carga local medida en N95 | no repetida tras retirada del asset grande | `Model` 0.260 s |
| Memoria local medida en N95 | no repetida tras retirada del asset grande | 137.8 MB RSS |
| Calidad esperada | superior | inferior a V2, pero suficiente para notas rapidas |
| Frase dictada en S25 | pendiente | pendiente |

## Pruebas pendientes en Galaxy S25

`adb` esta disponible en `/home/n95/Android/Sdk/platform-tools/adb`, pero no habia ningun dispositivo conectado/autorizado durante esta ejecucion, asi que no se pudo instalar ni probar fisicamente en el S25 desde consola.

Pendiente al probar en el telefono:

1. Desinstalar la version anterior o borrar datos.
2. Instalar V2.
3. Conceder microfono.
4. Confirmar copia completa del modelo.
5. Confirmar en Logcat `graph/words.txt`.
6. Dictar: `Llamar manana al laboratorio para revisar el caso del paciente`.
7. Guardar.
8. Cerrar y abrir.
9. Probar copiar, editar y eliminar.
10. Repetir en modo avion.
11. Probar apertura desde pantalla bloqueada.
12. Medir una nota de diez segundos y otra de un minuto.
13. Reiniciar telefono y probar tras el primer desbloqueo.
