# Executor Workspace

Espacio web adaptable para escribir, guardar, importar y exportar scripts Lua y buscar en ScriptBlox.

## Iniciar

Requiere Node.js 18 o superior:

```sh
npm start
```

Abre http://localhost:3000. La búsqueda usa un proxy del servidor hacia la API pública de ScriptBlox y necesita acceso a Internet. Los scripts y el borrador se almacenan en el navegador del dispositivo; borrar los datos del navegador puede eliminarlos. En navegadores compatibles se puede seleccionar una carpeta (por ejemplo, `Executor`) para escribir archivos `.lua` al guardar. La selección de carpeta dura la sesión; en otros navegadores utiliza la exportación JSON.

**Limitaciones:** es una aplicación web, no se conecta a Roblox ni ejecuta o inyecta scripts en el cliente. Tampoco puede permanecer como panel flotante sobre Roblox o funcionar en segundo plano como aplicación nativa. No proporciona integración con Rscripts.

## Android y descarga del APK

El proyecto `android/` empaqueta la interfaz en una WebView y requiere Android 8.0 o superior. El workflow **Compilar APK Android** se ejecuta en cada push y también manualmente desde GitHub Actions. Al terminar, descarga el artefacto **Executor-Workspace-Android** de la ejecución y extrae `app-debug.apk`. Es un APK de depuración, no una versión firmada para publicación. GitHub solo mostrará el workflow una vez que estos archivos se suban al repositorio.

La aplicación Android usa almacenamiento local de WebView, que se elimina al desinstalar o borrar los datos. La búsqueda requiere Internet y depende de ScriptBlox. En Config puedes seleccionar con el selector de Android una carpeta existente llamada `Executor` (o crearla allí); los próximos scripts guardados se escriben en esa carpeta. El permiso se conserva entre sesiones. La descarga/exportación JSON puede no estar disponible en WebView; usa la versión web en un navegador compatible para esa operación. Importar usa el selector de archivos de Android. No incluye ejecución/inyección en Roblox, superposición flotante ni funcionamiento en segundo plano.
