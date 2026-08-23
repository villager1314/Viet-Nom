const {app,BrowserWindow,ipcMain,shell}=require('electron');
const path=require('path');
let tts;
function wav(samples,rate){const pcm=Buffer.alloc(samples.length*2);for(let i=0;i<samples.length;i++){const x=Math.max(-1,Math.min(1,samples[i]));pcm.writeInt16LE(Math.round(x*32767),i*2)}const h=Buffer.alloc(44);h.write('RIFF');h.writeUInt32LE(36+pcm.length,4);h.write('WAVE',8);h.write('fmt ',12);h.writeUInt32LE(16,16);h.writeUInt16LE(1,20);h.writeUInt16LE(1,22);h.writeUInt32LE(rate,24);h.writeUInt32LE(rate*2,28);h.writeUInt16LE(2,32);h.writeUInt16LE(16,34);h.write('data',36);h.writeUInt32LE(pcm.length,40);return Buffer.concat([h,pcm]).toString('base64')}
function getTts(){if(tts)return tts;const sherpa=require('sherpa-onnx-node');const root=app.isPackaged?path.join(process.resourcesPath,'model'):path.join(__dirname,'assets','vits-piper-vi_VN-vais1000-medium');tts=new sherpa.OfflineTts({model:{vits:{model:path.join(root,'vi_VN-vais1000-medium.onnx'),tokens:path.join(root,'tokens.txt'),dataDir:path.join(root,'espeak-ng-data'),noiseScale:.667,noiseScaleW:.8,lengthScale:1.08},numThreads:2,provider:'cpu'},maxNumSentences:1,silenceScale:.25});return tts}
ipcMain.handle('tts-generate',async(_e,text)=>{const a=getTts().generate({text,sid:0,speed:.93,enableExternalBuffer:false});return wav(a.samples,a.sampleRate)});
ipcMain.handle('open-url',(_e,url)=>{if(/^https:\/\//.test(url))shell.openExternal(url)});
function create(){const w=new BrowserWindow({width:1180,height:820,minWidth:760,minHeight:620,backgroundColor:'#f2eee4',autoHideMenuBar:true,webPreferences:{preload:path.join(__dirname,'preload.js'),contextIsolation:true,nodeIntegration:false}});w.loadFile(path.join(__dirname,'assets','viet-nom.html'))}
app.whenReady().then(create);app.on('window-all-closed',()=>{if(process.platform!=='darwin')app.quit()});

