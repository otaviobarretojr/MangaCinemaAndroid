'use strict';
/* Gabriel Lê – biblioteca ilustrada. Sem servidor: tudo fica neste navegador. */
const DB_NAME = 'gabriel-le-biblioteca';
const DB_VERSION = 1;
const STORE = 'dados';
const uid = () => 'g' + (globalThis.crypto?.randomUUID?.() || `${Date.now()}_${Math.random().toString(36).slice(2,10)}`);
const escapeHTML = v => String(v ?? '').replace(/[&<>"']/g, ch => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
const htmlLine = v => escapeHTML(v).replace(/\n/g, '<br>');
const DEMO_BOOK = {
 id:'gabriel-portal-01',
 title:'Gabriel e o Portal Misterioso',
 subtitle:'Uma aventura de amizade e coragem',
 description:'Gabriel, Luna, Theo e Floquinho descobrem um portal que leva a um novo mundo de blocos.',
 category:'Aventura • 6 anos',
 cover:'',
 pages:[
  {id:'p01',type:'cover',scene:'portal',title:'Gabriel e o Portal Misterioso',text:'Uma aventura de amizade e coragem.\nPreparado para entrar no portal?'},
  {id:'p02',type:'story',scene:'equipe',title:'Conheça a turma',text:'Gabriel é curioso e corajoso.\nLuna tem ideias criativas.\nTheo adora construir.\nFloquinho é um lobo muito fiel!'},
  {id:'p03',type:'story',scene:'floresta',title:'Um dia de aventura',text:'Gabriel chamou Luna e Theo para brincar.\nFloquinho foi junto.\nTodos correram para a floresta.'},
  {id:'p04',type:'story',scene:'portal',title:'Uma luz roxa',text:'De repente, Gabriel viu uma luz.\nEla vinha de um portal roxo!\n— Que lugar será esse?'},
  {id:'p05',type:'story',scene:'portal',title:'Três, dois, um!',text:'Os amigos deram as mãos.\nGabriel contou até três.\nE todos entraram no portal!'},
  {id:'p06',type:'story',scene:'ilha',title:'Tudo era diferente',text:'Do outro lado, havia ilhas no céu, árvores roxas e rios brilhantes.\nEra um mundo feito de blocos!'},
  {id:'p07',type:'story',scene:'floresta',title:'Uma trilha secreta',text:'Floquinho farejou o chão.\nEle achou uma trilha de luz.\nOs três amigos foram atrás dele.'},
  {id:'p08',type:'story',scene:'castelo',title:'O castelo escuro',text:'A trilha levava a um castelo.\nLá, sombras de pedra se mexiam.\nOs amigos ficaram bem juntinhos.'},
  {id:'p09',type:'story',scene:'equipe',title:'Um plano de equipe',text:'— Tenho uma ideia! — disse Gabriel.\nTheo construiu um muro.\nLuna abriu um caminho de blocos.'},
  {id:'p10',type:'story',scene:'cristal',title:'O Coração do Mundo',text:'No castelo, havia um grande cristal.\nEle estava perdendo a sua luz.\nO mundo precisava de ajuda!'},
  {id:'p11',type:'story',scene:'castelo',title:'O Guardião apareceu',text:'BUM! Surgiu um gigante de sombra.\nEle queria guardar toda a luz.\nMas Gabriel não deixou os amigos sós.'},
  {id:'p12',type:'story',scene:'equipe',title:'A grande missão',text:'O Guardião lançou sombras no chão.\nGabriel protegeu os amigos.\nLuna e Theo correram até o cristal.'},
  {id:'p13',type:'story',scene:'cristal',title:'A luz voltou!',text:'Floquinho achou uma pedra de luz.\nOs amigos a levaram ao cristal.\nPLIM! Todo o castelo brilhou!'},
  {id:'p14',type:'story',scene:'portal',title:'O portal se abriu',text:'As sombras foram embora.\nO portal voltou a aparecer.\nEra hora de voltar para casa.'},
  {id:'p15',type:'story',scene:'floresta',title:'De volta ao lar',text:'Os amigos saíram do portal.\nA floresta estava em paz.\nGabriel abraçou Floquinho.'},
  {id:'p16',type:'story',scene:'sol',title:'O maior tesouro',text:'Gabriel olhou para seus amigos.\nEle tinha aprendido uma lição:\njuntos, somos mais fortes!'},
  {id:'p17',type:'moral',scene:'equipe',title:'O que aprendemos?',text:'CORAGEM é tentar, mesmo com medo.\nAMIZADE é cuidar dos amigos.\nEQUIPE é ajudar e aceitar ajuda.'},
  {id:'p18',type:'quiz',scene:'quiz',title:'Missão especial!',text:'Responda às perguntas e conquiste sua estrela.',questions:[
   {question:'De que cor era o portal?',options:['Roxo','Verde','Azul'],correct:0},
   {question:'Quem era o lobo da turma?',options:['Theo','Floquinho','Gabriel'],correct:1},
   {question:'O que ajudou os amigos?',options:['Brigar','Trabalhar juntos','Desistir'],correct:1}
  ]},
  {id:'p19',type:'end',scene:'sol',title:'Fim... por enquanto!',text:'Qual será a próxima aventura?\nContinue imaginando. Continue lendo!'}
 ]
};
let data = {books:[DEMO_BOOK],positions:{},prefs:{fontScale:1,theme:'light',motion:true}};
let current = {view:'home',bookId:null};
let quizAnswers = {};
let db = null;
let saveTimer = null;
let toastTimer = null;
let deferredInstall = null;
const viewEl = document.getElementById('view');
const bottomNav = document.getElementById('bottomNav');
const backBtn = document.getElementById('backBtn');
const homeBtn = document.getElementById('homeBtn');
const importInput = document.getElementById('importInput');

function openDB() {
 return new Promise((resolve, reject)=>{
  if(!('indexedDB' in window)) return reject(new Error('IndexedDB indisponível'));
  const request = indexedDB.open(DB_NAME, DB_VERSION);
  request.onupgradeneeded = () => request.result.createObjectStore(STORE);
  request.onsuccess = () => resolve(request.result);
  request.onerror = () => reject(request.error);
 });
}
function getState() {
 return new Promise((resolve,reject)=>{
  const req = db.transaction(STORE,'readonly').objectStore(STORE).get('library');
  req.onsuccess = () => resolve(req.result);
  req.onerror = () => reject(req.error);
 });
}
function persist() {
 if(!db) { try{localStorage.setItem(DB_NAME,JSON.stringify(data));}catch(e){toast('Armazenamento cheio. Exporte um backup.',true);} return Promise.resolve(); }
 return new Promise((resolve,reject)=>{
  const tx=db.transaction(STORE,'readwrite');
  tx.objectStore(STORE).put(data,'library');
  tx.oncomplete=()=>resolve();
  tx.onerror=()=>{toast('Não foi possível salvar. Exporte um backup.',true);reject(tx.error);};
 });
}
function queueSave() { clearTimeout(saveTimer);saveTimer=setTimeout(()=>persist().catch(console.error),450); }
function toast(message,isError=false) {
 const el=document.getElementById('toast');el.textContent=message;el.classList.add('show');el.style.background=isError?'#fff0f0':'#ebfff4';
 clearTimeout(toastTimer);toastTimer=setTimeout(()=>el.classList.remove('show'),3200);
}
function getBook(id){return data.books.find(b=>b.id===id);}
function getPosition(b){return Math.max(0,Math.min(b.pages.length-1,Number(data.positions[b.id])||0));}
function pct(b){return b.pages.length>0?Math.round(((getPosition(b)+1)/b.pages.length)*100):0;}
function art(scene='portal',image='',showTag=false){
 const visual=image?`<img loading="eager" src="${escapeHTML(image)}" class="motion-image" alt="Ilustração da página">`:`<div class="scene-art" data-scene="${escapeHTML(scene)}"><div class="sparkle"></div><div class="sparkle"></div><div class="scene-symbol">?</div></div>`;
 return `<div class="motion-frame" data-motion-scene="${escapeHTML(scene)}">${visual}<span class="motion-glow"></span><span class="motion-particle p1"></span><span class="motion-particle p2"></span><span class="motion-particle p3"></span><span class="motion-particle p4"></span></div>${!image&&showTag?'<span class="art-placeholder-tag">ILUSTRAÇÃO EM PREPARAÇÃO</span>':''}`;
}

function navigate(view, bookId=null, push=true){
 if(window.speechSynthesis) speechSynthesis.cancel();
 current={view,bookId};
 if(push){try{history.pushState({view,bookId},'',location.pathname+location.search+(view==='home'?'':'#'+view));}catch(e){/* origem local ou de testes */}}
 render();window.scrollTo(0,0);
}
function render(){
 document.body.classList.toggle('reader',current.view==='reader');
 document.body.classList.toggle('motion-off',data.prefs.motion===false);
 const isFull=['reader','editor'].includes(current.view);
 bottomNav.classList.toggle('hidden',isFull);
 backBtn.classList.toggle('hidden',!isFull);
 [...bottomNav.querySelectorAll('.nav-btn')].forEach(n=>n.classList.toggle('active',n.dataset.nav===current.view));
 if(current.view==='reader')return renderReader();
 if(current.view==='parents')return renderParents();
 if(current.view==='editor')return renderEditor();
 return renderHome();
}
function renderHome(){
 const books=data.books.map(b=>{
 const pos=getPosition(b);
 return `<button class="book-card" data-action="open-book" data-book="${escapeHTML(b.id)}" aria-label="Abrir ${escapeHTML(b.title)}">
  <div class="book-visual">${art(b.pages?.[0]?.scene||'portal',b.cover)}<div class="book-cover-title">${escapeHTML(b.title)}</div></div>
  <div class="book-meta"><strong>${escapeHTML(b.subtitle||'Uma nova aventura')}</strong><p>${escapeHTML(b.description||'Venha descobrir esta história.')}</p>
   <div class="book-meta-foot"><span>${pos===0?'COMEÇAR A LER':'CONTINUAR LENDO'}</span><span>${escapeHTML(String(b.pages.length))} PÁGINAS ↗</span></div>
   <div class="mini-progress"><i style="width:${pct(b)}%"></i></div>
  </div></button>`;
 }).join('');
 viewEl.innerHTML=`<section class="hero"><div><span class="pill">✦ UM MUNDO DE HISTÓRIAS</span><h1 class="hero-title">Olá, Gabriel!<br>Pronto para uma <em>aventura?</em></h1><p class="section-sub">Escolha um livro, vire as páginas e descubra mundos incríveis. Cada história é uma nova conquista!</p></div><div class="hero-art">${art('portal')}</div></section>
 <div class="section-heading"><h2>📚 Sua biblioteca</h2><small>${data.books.length} ${data.books.length===1?'livro':'livros'}</small></div>
 <div class="book-grid">${books||'<div class="empty">Ainda não há livros. Peça para um responsável importar uma história.</div>'}</div>
 <p class="placeholder-note">✨ <strong>Nova edição em produção:</strong> as ilustrações da aventura serão adicionadas página por página. O texto já pode ser lido nesta versão de testes.</p>`;
}
function renderReader(){
 const book=getBook(current.bookId);if(!book){return navigate('home',null,false);}
 const pos=getPosition(book),page=book.pages[pos];
 if(!page){return navigate('home',null,false);}
 const isQuiz=page.type==='quiz';
 let pageContent;
 if(isQuiz){
  const questions=Array.isArray(page.questions)?page.questions:[];
  pageContent=`<div class="quiz"><span class="eyebrow">⭐ MISSÃO DE LEITURA</span><h2>${escapeHTML(page.title)}</h2>${questions.map((q,i)=>{
    const answered=quizAnswers[`${book.id}_${pos}_${i}`];
    return `<div class="quiz-question"><p>${i+1}. ${escapeHTML(q.question)}</p>${q.options.map((opt,j)=>{
      const status=answered===j?(j===q.correct?' correct':' wrong'):'';
      return `<button class="quiz-option${status}" data-action="answer" data-question="${i}" data-option="${j}">${escapeHTML(opt)}</button>`;
    }).join('')}${answered!==undefined?`<small class="quiz-feedback">${answered===q.correct?'🌟 Parabéns! Você acertou!':'Quase! Tente outra resposta.'}</small>`:''}</div>`;
   }).join('')}</div>`;
 }else{
  pageContent=`<div class="page-illustration">${art(page.scene,page.image,true)}</div><div class="page-story"><h2>${escapeHTML(page.title)}</h2><p>${escapeHTML(page.text)}</p></div>`;
 }
 viewEl.innerHTML=`<div class="reader-wrap"><div class="reader-info"><span class="reader-kicker">${escapeHTML(book.title)}</span><span class="reader-count">PÁGINA ${pos+1} / ${book.pages.length}</span></div>
 <div class="progress-track" aria-label="Progresso da leitura"><span style="width:${pct(book)}%"></span></div>
 <section class="page-sheet" id="storyPage" aria-label="Página ${pos+1}">${pageContent}</section>
 <div class="reader-tools"><button class="tool-button" data-action="toggle-motion" aria-pressed="${data.prefs.motion!==false}">${data.prefs.motion!==false?'✨ Movimento: ligado':'◯ Movimento: desligado'}</button><button class="tool-button" data-action="speak" ${isQuiz?'disabled':''}>🔊 Ouvir página</button><div class="actions"><button class="tool-button" data-action="font-minus" aria-label="Diminuir letra">A−</button><button class="tool-button" data-action="font-plus" aria-label="Aumentar letra">A+</button></div></div>
 <div class="page-nav"><button class="btn secondary" data-action="prev" ${pos===0?'disabled':''}>← Anterior</button><button class="btn" data-action="next">${pos===book.pages.length-1?'📚 Biblioteca':'Próxima →'}</button></div>
 <p class="footnote" style="text-align:center">Deslize a imagem para mudar de página. Seu progresso é salvo neste dispositivo.</p></div>`;
 document.documentElement.style.setProperty('--font-scale',String(data.prefs.fontScale||1));
 attachSwipe();
}
function changePage(delta){
 const b=getBook(current.bookId);if(!b)return;
 const next=getPosition(b)+delta;
 if(next>=b.pages.length){navigate('home');return;}
 data.positions[b.id]=Math.max(0,next);queueSave();renderReader();window.scrollTo({top:0,behavior:'instant'});
}
function attachSwipe(){
 const zone=document.getElementById('storyPage');if(!zone)return;
 let x=null,y=null;
 zone.addEventListener('touchstart',e=>{x=e.changedTouches[0].clientX;y=e.changedTouches[0].clientY;},{passive:true});
 zone.addEventListener('touchend',e=>{if(x===null)return;const dx=e.changedTouches[0].clientX-x,dy=e.changedTouches[0].clientY-y;if(Math.abs(dx)>65&&Math.abs(dx)>Math.abs(dy)*1.4)changePage(dx<0?1:-1);x=null;},{passive:true});
}
function renderParents(){
 viewEl.innerHTML=`<div class="parent-head"><div><span class="eyebrow">ESPAÇO DA FAMÍLIA</span><h1>Área dos pais ⚙</h1><div class="section-sub">Adicione livros, organize páginas e importe uma ilustração por vez.</div></div></div>
 <div class="form-card"><h2>Adicionar histórias</h2><p>As histórias ficam salvas somente neste navegador. Importe um arquivo de livro ou crie uma nova aventura.</p><div class="actions"><button class="btn" data-action="new-book">＋ Novo livro</button><button class="btn secondary" data-action="import-book">↥ Importar história</button><button class="btn ghost" data-action="export-library">↓ Backup de tudo</button></div></div>
 <div class="section-heading"><h2>Livros cadastrados</h2><small>${data.books.length}</small></div>
 ${data.books.map(b=>`<div class="parent-book"><div><strong>${escapeHTML(b.title)}</strong><small>${b.pages.length} páginas · ${b.pages.filter(p=>!!p.image).length} imagens carregadas</small></div><div class="actions"><button class="btn small secondary" data-action="edit-book" data-book="${escapeHTML(b.id)}">Editar</button><button class="btn small ghost" data-action="export-book" data-book="${escapeHTML(b.id)}" title="Exportar livro">↓</button><button class="btn small danger" data-action="delete-book" data-book="${escapeHTML(b.id)}" title="Excluir livro">×</button></div></div>`).join('')}
 <div class="form-card"><h3>Como funciona?</h3><p><strong>1.</strong> Crie a história e suas páginas.<br><strong>2.</strong> Adicione a imagem de cada página, uma por uma.<br><strong>3.</strong> O texto fica separado da ilustração para nunca ser cortado.<br><strong>4.</strong> Importe novos livros quando quiser, sem apagar os antigos.</p>
 <p class="footnote">🔒 Não há anúncios nem envio das histórias para servidores. Para ter a biblioteca em outros aparelhos, exporte um backup e importe no segundo aparelho. A instalação como aplicativo exige publicação em HTTPS.</p></div>`;
}
function renderEditor(){
 const b=getBook(current.bookId);if(!b)return navigate('parents',null,false);
 viewEl.innerHTML=`<div class="parent-head"><div><span class="eyebrow">EDITANDO LIVRO</span><h1>${escapeHTML(b.title)}</h1><p class="section-sub">Tudo que você alterar é salvo automaticamente neste aparelho.</p></div><button class="btn secondary small" data-action="export-book" data-book="${escapeHTML(b.id)}">↓ Exportar</button></div>
 <div class="form-card"><h2>Informações do livro</h2>
 <label class="field"><span>Título do livro</span><input data-book-field="title" value="${escapeHTML(b.title)}" maxlength="140"></label>
 <label class="field"><span>Subtítulo</span><input data-book-field="subtitle" value="${escapeHTML(b.subtitle||'')}" maxlength="160"></label>
 <label class="field"><span>Sinopse</span><textarea data-book-field="description">${escapeHTML(b.description||'')}</textarea></label>
 <label class="field"><span>Capa ilustrada (opcional)</span><input type="file" accept="image/png,image/jpeg,image/webp" data-cover-upload><small class="helper">É melhor gerar a arte sem palavras; o título aparece automaticamente.</small></label>
 ${b.cover?`<div class="image-mini-preview"><img src="${escapeHTML(b.cover)}" alt="Capa cadastrada">Capa salva <button class="btn small ghost" data-action="remove-cover">Remover</button></div>`:''}
 </div>
 <div class="section-heading"><h2>Páginas (${b.pages.length})</h2><button class="btn small" data-action="add-page">＋ Nova página</button></div>
 ${b.pages.map((p,i)=>`<details class="page-editor" id="editor-page-${i}"><summary>${i+1}. ${escapeHTML(p.title||'Sem título')} ${p.image?'🖼️':'○'}</summary>
 <div class="page-editor-body">
 <label class="field"><span>Tipo da página</span><select data-page-field="type" data-index="${i}">${['cover','story','moral','quiz','end'].map(k=>`<option value="${k}" ${p.type===k?'selected':''}>${{cover:'Capa',story:'História',moral:'Aprendizado',quiz:'Perguntas',end:'Final'}[k]}</option>`).join('')}</select></label>
 <label class="field"><span>Título</span><input data-page-field="title" data-index="${i}" maxlength="160" value="${escapeHTML(p.title)}"></label>
 <label class="field"><span>Texto da página</span><textarea rows="4" data-page-field="text" data-index="${i}">${escapeHTML(p.text||'')}</textarea><small class="helper">O texto aparece nítido sobre um fundo claro, fora da imagem.</small></label>
 <label class="field"><span>Fundo provisório</span><select data-page-field="scene" data-index="${i}">${['portal','floresta','ilha','castelo','cristal','equipe','sol','quiz'].map(k=>`<option value="${k}" ${p.scene===k?'selected':''}>${{portal:'Portal roxo',floresta:'Floresta',ilha:'Ilhas flutuantes',castelo:'Castelo',cristal:'Cristal',equipe:'Amigos',sol:'Pôr do sol',quiz:'Desafio'}[k]}</option>`).join('')}</select></label>
 <label class="field"><span>Importar ilustração desta página</span><input type="file" accept="image/png,image/jpeg,image/webp" data-image-upload="${i}"><small class="helper">PNG, JPEG ou WebP. A imagem será otimizada para o celular.</small></label>
 <div class="image-mini-preview">${p.image?`<img src="${escapeHTML(p.image)}" alt="Imagem da página ${i+1}">Imagem salva`:'◻ Esta página ainda não tem imagem definitiva.'}</div>
 ${p.image?`<button class="btn small ghost" data-action="remove-image" data-index="${i}">Remover imagem</button>`:''}
 ${p.type==='quiz'?`<label class="field"><span>Perguntas (uma por linha)</span><textarea rows="5" data-page-field="questionsText" data-index="${i}">${escapeHTML(encodeQuestions(p.questions||[]))}</textarea><small class="helper">Formato: Pergunta | opção 1 | opção 2 | opção 3 | número da resposta (1, 2 ou 3)</small></label>`:''}
 <div class="actions"><button class="btn small secondary" data-action="page-up" data-index="${i}" ${i===0?'disabled':''}>↑ Subir</button><button class="btn small secondary" data-action="page-down" data-index="${i}" ${i===b.pages.length-1?'disabled':''}>↓ Descer</button><button class="btn small danger" data-action="delete-page" data-index="${i}">Excluir página</button></div>
 </div></details>`).join('')}
 <div class="form-card"><div class="actions"><button class="btn" data-action="add-page">＋ Nova página</button><button class="btn secondary" data-action="read-book" data-book="${escapeHTML(b.id)}">▶ Testar leitura</button><button class="btn ghost" data-action="export-book" data-book="${escapeHTML(b.id)}">↓ Exportar livro</button></div><p class="footnote">Dica: salve uma cópia da história exportando o livro após incluir as imagens.</p></div>`;
}
function encodeQuestions(questions){return questions.map(q=>[q.question,...q.options,(q.correct||0)+1].join(' | ')).join('\n');}
function parseQuestions(text){
 const lines=String(text).split('\n').map(x=>x.trim()).filter(Boolean);
 return lines.map(line=>{
  const x=line.split('|').map(s=>s.trim());
  if(x.length!==5 || ![1,2,3].includes(Number(x[4])))throw new Error('Confira o formato das perguntas.');
  return {question:x[0],options:x.slice(1,4),correct:Number(x[4])-1};
 });
}
function normalizeImage(v){return typeof v==='string' && /^data:image\/(png|jpeg|webp);base64,[a-zA-Z0-9+/=]+$/.test(v)?v:'';}
function sanitizeBook(book){
 if(!book || typeof book!=='object'|| !Array.isArray(book.pages)||!book.pages.length||book.pages.length>250)throw new Error('Livro inválido: confira a lista de páginas (até 250).');
 const newBook={id:/^[a-zA-Z0-9_-]{1,80}$/.test(String(book.id||''))?String(book.id):uid(),title:String(book.title||'Livro sem título').slice(0,140),subtitle:String(book.subtitle||'').slice(0,160),description:String(book.description||'').slice(0,600),category:String(book.category||'Aventura').slice(0,100),cover:normalizeImage(book.cover),pages:[]};
 newBook.pages=book.pages.map((p,i)=>{
  const kinds=['cover','story','moral','quiz','end']; const scenes=['portal','floresta','ilha','castelo','cristal','equipe','sol','quiz'];
  return {id:String(p.id||uid()).slice(0,100),type:kinds.includes(p.type)?p.type:'story',scene:scenes.includes(p.scene)?p.scene:'portal',title:String(p.title||`Página ${i+1}`).slice(0,160),text:String(p.text||'').slice(0,2500),image:normalizeImage(p.image),questions:Array.isArray(p.questions)?p.questions.slice(0,15).map(q=>({question:String(q.question||'').slice(0,300),options:Array.isArray(q.options)?q.options.slice(0,3).map(o=>String(o).slice(0,150)):['A','B','C'],correct:[0,1,2].includes(Number(q.correct))?Number(q.correct):0})):[]};
 });
 return newBook;
}
function download(filename,obj){
 const blob=new Blob([JSON.stringify(obj,null,2)],{type:'application/json'});
 const url=URL.createObjectURL(blob),a=document.createElement('a');a.href=url;a.download=filename;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),45000);
}
function fileToImage(file){
 return new Promise((resolve,reject)=>{
  if(!file||!['image/png','image/jpeg','image/webp'].includes(file.type))return reject(new Error('Use uma imagem PNG, JPG ou WebP.'));
  if(file.size>25*1024*1024)return reject(new Error('Imagem acima do limite de 25 MB.'));
  const image=new Image();const blobUrl=URL.createObjectURL(file);
  image.onload=()=>{
   try{
    const factor=Math.min(1,1800/image.naturalWidth,2400/image.naturalHeight);
    const w=Math.max(1,Math.round(image.naturalWidth*factor)),h=Math.max(1,Math.round(image.naturalHeight*factor));
    const canvas=document.createElement('canvas');canvas.width=w;canvas.height=h;
    canvas.getContext('2d').drawImage(image,0,0,w,h);
    let url=canvas.toDataURL('image/webp',.88);
    if(!url.startsWith('data:image/webp;'))url=canvas.toDataURL('image/jpeg',.9);
    URL.revokeObjectURL(blobUrl);resolve(url);
   }catch(err){URL.revokeObjectURL(blobUrl);reject(err);}
  };
  image.onerror=()=>{URL.revokeObjectURL(blobUrl);reject(new Error('Não foi possível abrir a imagem.'));};
  image.src=blobUrl;
 });
}
function speak(){
 if(!('speechSynthesis' in window)){toast('Este navegador não oferece leitura em voz alta.',true);return;}
 const b=getBook(current.bookId);if(!b)return;const p=b.pages[getPosition(b)];
 if(speechSynthesis.speaking){speechSynthesis.cancel();toast('Narração pausada');return;}
 const utterance=new SpeechSynthesisUtterance([p.title,p.text].filter(Boolean).join('. '));
 utterance.lang='pt-BR';utterance.rate=.87;utterance.pitch=1.04;
 const voice=speechSynthesis.getVoices().find(v=>v.lang.toLowerCase()==='pt-br');if(voice)utterance.voice=voice;
 speechSynthesis.speak(utterance);toast('🔊 Lendo a página');
}
async function importFile(file){
 if(file.size>65*1024*1024)throw new Error('Arquivo muito grande (máximo 65 MB).');
 const imported=JSON.parse(await file.text());
 const incoming=Array.isArray(imported.books)?imported.books:Array.isArray(imported)?imported:[imported];
 if(!incoming.length||incoming.length>50)throw new Error('Nenhum livro encontrado.');
 const normalized=incoming.map(sanitizeBook);
 let count=0;
 for(const b of normalized){
  const index=data.books.findIndex(old=>old.id===b.id);
  if(index>=0){
   if(!confirm(`O livro "${b.title}" já existe. Quer atualizar a versão salva?`))continue;
   data.books[index]=b;
  }else data.books.unshift(b);
  count++;
 }
 await persist();render();toast(`${count} ${count===1?'livro importado':'livros importados'} com sucesso.`);
}
async function handleAction(target){
 const action=target.dataset.action;
 const bookId=target.dataset.book||current.bookId;
 const b=getBook(bookId);
 const i=Number(target.dataset.index);
 switch(action){
  case 'open-book':case 'read-book': navigate('reader',bookId);break;
  case 'prev':changePage(-1);break;
  case 'next':changePage(1);break;
  case 'font-plus':data.prefs.fontScale=Math.min(1.5,Number((data.prefs.fontScale+.1).toFixed(2)));queueSave();renderReader();break;
  case 'font-minus':data.prefs.fontScale=Math.max(.85,Number((data.prefs.fontScale-.1).toFixed(2)));queueSave();renderReader();break;
  case 'speak':speak();break;
  case 'toggle-motion':data.prefs.motion=data.prefs.motion===false;queueSave();render();toast(data.prefs.motion?'Movimento ativado!':'Movimento desativado.');break;
  case 'answer':{
   if(!b)return;const pos=getPosition(b),qi=Number(target.dataset.question),answer=Number(target.dataset.option);
   quizAnswers[`${b.id}_${pos}_${qi}`]=answer;renderReader();break;
  }
  case 'new-book':{
   const title=prompt('Como será o título da próxima aventura?','Uma Nova Aventura');
   if(!title?.trim())return;
   const nb={id:uid(),title:title.trim().slice(0,140),subtitle:'Uma nova aventura do Gabriel',description:'Uma história para descobrir juntos.',cover:'',pages:[{id:uid(),type:'cover',scene:'portal',title:title.trim(),text:'Era uma vez...',image:''}]};
   data.books.unshift(nb);await persist();toast('Livro criado!');navigate('editor',nb.id);break;
  }
  case 'edit-book':navigate('editor',bookId);break;
  case 'delete-book':{
   if(!b)return;
   if(!confirm(`Excluir definitivamente o livro "${b.title}"? Exporte um backup antes de excluir.`))return;
   data.books=data.books.filter(x=>x.id!==b.id);delete data.positions[b.id];await persist();renderParents();toast('Livro excluído.');break;
  }
  case 'import-book':importInput.value='';importInput.click();break;
  case 'export-book':if(b){download(`${b.id}.gbrbook`,b);toast('Livro exportado. Guarde uma cópia!');}break;
  case 'export-library':download('biblioteca-gabriel-backup.json',{format:'gabriel-le',version:1,exportedAt:new Date().toISOString(),books:data.books});toast('Backup criado.');break;
  case 'add-page':{
   if(!b)return;
   b.pages.push({id:uid(),type:'story',scene:'portal',title:`Página ${b.pages.length+1}`,text:'Escreva aqui a próxima parte da aventura.',image:''});
   await persist();renderEditor();document.getElementById(`editor-page-${b.pages.length-1}`)?.setAttribute('open','');toast('Nova página criada.');break;
  }
  case 'delete-page':{
   if(!b||b.pages.length<=1){toast('O livro precisa ter pelo menos uma página.',true);return;}
   if(!confirm(`Excluir a página ${i+1}?`))return;
   b.pages.splice(i,1);data.positions[b.id]=Math.max(0,Math.min(getPosition(b),b.pages.length-1));await persist();renderEditor();break;
  }
  case 'page-up':case 'page-down':{
   const j=i+(action==='page-up'?-1:1);if(!b||i<0||j<0||j>=b.pages.length)return;
   [b.pages[i],b.pages[j]]=[b.pages[j],b.pages[i]];await persist();renderEditor();document.getElementById(`editor-page-${j}`)?.setAttribute('open','');break;
  }
  case 'remove-image':if(b&&b.pages[i]&&confirm('Remover a ilustração desta página?')){b.pages[i].image='';await persist();renderEditor();}break;
  case 'remove-cover':if(b&&confirm('Remover a capa do livro?')){b.cover='';await persist();renderEditor();}break;
  default:break;
 }
}
async function handleFileChange(el){
 const file=el.files?.[0];if(!file)return;
 const b=getBook(current.bookId);if(!b)return;
 try{
  toast('Otimizando e salvando imagem...');
  const image=await fileToImage(file);
  let pageIndex=null;
  if(el.hasAttribute('data-cover-upload'))b.cover=image;
  else {pageIndex=Number(el.dataset.imageUpload);if(!b.pages[pageIndex])return;b.pages[pageIndex].image=image;}
  await persist();renderEditor();
  if(pageIndex!==null)document.getElementById(`editor-page-${pageIndex}`)?.setAttribute('open','');
  toast('Imagem importada com sucesso!');
 }catch(e){toast(e.message,true);}
}
function installEvents(){
 document.addEventListener('click',event=>{
  const target=event.target.closest('[data-action]');
  if(target){event.preventDefault();handleAction(target).catch(e=>toast(e.message,true));}
 });
 document.addEventListener('input',event=>{
  const el=event.target;
  const b=getBook(current.bookId);
  if(!b)return;
  if(el.hasAttribute('data-book-field')){b[el.dataset.bookField]=el.value;queueSave();}
  if(el.hasAttribute('data-page-field')&&el.dataset.pageField!=='questionsText'){
   const p=b.pages[Number(el.dataset.index)];if(p){p[el.dataset.pageField]=el.value;queueSave();}
  }
 });
 document.addEventListener('change',event=>{
  const el=event.target;
  if(el.matches('[data-image-upload],[data-cover-upload]'))return handleFileChange(el);
  if(el.dataset.pageField==='questionsText'){
   const b=getBook(current.bookId);if(!b)return;
   try{b.pages[Number(el.dataset.index)].questions=parseQuestions(el.value);queueSave();toast('Perguntas salvas.');}
   catch(e){toast(e.message,true);}
  }
  if(el.dataset.pageField==='type')renderEditor();
 });
 importInput.addEventListener('change',async event=>{
  const file=event.target.files?.[0];if(!file)return;
  try{await importFile(file);}catch(e){toast(`Falha na importação: ${e.message}`,true);}
 });
 document.querySelectorAll('[data-nav]').forEach(el=>el.addEventListener('click',()=>navigate(el.dataset.nav)));
 backBtn.addEventListener('click',()=>navigate(current.view==='editor'?'parents':'home'));
 homeBtn.addEventListener('click',()=>navigate('home'));
 window.addEventListener('keydown',event=>{
  if(current.view!=='reader'||/INPUT|TEXTAREA|SELECT/.test(document.activeElement.tagName))return;
  if(event.key==='ArrowRight')changePage(1);
  else if(event.key==='ArrowLeft')changePage(-1);
 });
 window.addEventListener('popstate',event=>{
  const s=event.state||{view:'home',bookId:null};
  current={view:s.view||'home',bookId:s.bookId||null};render();
 });
 window.addEventListener('beforeinstallprompt',e=>{e.preventDefault();deferredInstall=e;});
}
async function init(){
 installEvents();
 try{
  db=await openDB();const saved=await getState();
  if(saved&&Array.isArray(saved.books))data={books:saved.books.map(sanitizeBook),positions:saved.positions||{},prefs:{fontScale:1,motion:true,...(saved.prefs||{})}};
  else await persist();
 }catch(err){
  console.warn('Local storage fallback',err);
  try{const saved=JSON.parse(localStorage.getItem(DB_NAME));if(saved&&Array.isArray(saved.books))data=saved;}catch(e){}
  toast('Modo compatível: faça backups das histórias regularmente.',true);
 }
 try{history.replaceState({view:'home',bookId:null},'',location.pathname+location.search);}catch(e){/* origem local ou de testes */}
 render();
 if('serviceWorker' in navigator && location.protocol!=='file:')navigator.serviceWorker.register('./sw.js').catch(console.warn);
}
init();