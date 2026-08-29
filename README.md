# NotasVozLocal

APK Android local para grabar notas de voz y transcribirlas sin Internet.

## Instalacion

1. Copiar al Galaxy S25 el APK:
   `/home/n95/gDrive/Descargas_OpenClaw/APKs/NotasVozLocal_v0.7_simple_swipes_debug.apk`
2. Instalarlo permitiendo origen desconocido si Android lo solicita.
3. Abrir la app y conceder permiso de microfono.
4. En el primer arranque la app copia el modelo Vosk al almacenamiento privado interno. En los siguientes arranques no debe copiarlo otra vez si la version ya coincide.

## Uso

- Al abrir la app se muestra la lista de notas.
- Pulsar `Nueva nota de voz` para abrir captura.
- En captura, la app prepara el modelo y empieza a escuchar automaticamente.
- Pulsar `Guardar` para detener reconocimiento y guardar solo el texto con fecha y hora.
- Si se apaga la pantalla durante la captura, guarda automaticamente el texto actual y cierra.
- Cada nota tiene menu `...` con:
  - Copiar texto.
  - Compartir.
  - Editar.
  - Eliminar.
- En la lista, deslizar una nota hacia la derecha la elimina directamente.
- Deslizar hacia la izquierda abre el dialogo de compartir de Android.
- Tocar una nota sin deslizar abre edicion.

## Privacidad

- No usa cuentas.
- No usa nube.
- No usa Firebase.
- No guarda audio.
- La transcripcion se hace en local con Vosk.

## Entrega Vigente

- APK: `/home/n95/gDrive/Descargas_OpenClaw/APKs/NotasVozLocal_v0.7_simple_swipes_debug.apk`
- Version: `0.7-simple-swipes`
- Tamano APK: `53 MB`
- Arquitectura incluida: `arm64-v8a`
