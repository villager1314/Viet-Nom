const {contextBridge,ipcRenderer}=require('electron');
contextBridge.exposeInMainWorld('DesktopTTS',{generate:text=>ipcRenderer.invoke('tts-generate',text),open:url=>ipcRenderer.invoke('open-url',url)});

