<!-- app-release:start -->
[**Descargar APK actual v0.8**](https://github.com/ricardoyf/NotasVozLocal/releases/download/v0.8-ideas-icon/NotasVozLocal-v0.8-ideas-icon.apk) · [SHA-256](https://github.com/ricardoyf/NotasVozLocal/releases/download/v0.8-ideas-icon/NotasVozLocal-v0.8-ideas-icon.apk.sha256)

Versión objetivo conservada: [v0.7-simple-swipes](https://github.com/ricardoyf/NotasVozLocal/releases/tag/v0.7-simple-swipes).

`6f07b59b710d677a4f28ffe4ead509a34312c48578adbba421f3fb75bf4e9680`
<!-- app-release:end -->

# NotasVozLocal

APK Android local para grabar notas de voz y transcribirlas sin Internet.

## Instalacion

1. Descarga el APK actual desde el enlace al principio del README y cópialo al dispositivo.
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

## Entregas

- Actual: `0.8-ideas-icon` (APK enlazado al principio).
- Objetivo histórico conservado: `0.7-simple-swipes` (release independiente).
- Arquitectura incluida: `arm64-v8a`.
