export default {
 async fetch(request,env){
  const cors={ "Access-Control-Allow-Origin":"*", "Access-Control-Allow-Headers":"Content-Type,Authorization", "Access-Control-Allow-Methods":"GET,POST,PUT,OPTIONS" };
  if(request.method==="OPTIONS")return new Response(null,{headers:cors});
  const json=(data,status=200)=>new Response(JSON.stringify(data),{status,headers:{...cors,"Content-Type":"application/json; charset=utf-8"}});
  const url=new URL(request.url),path=url.pathname;
  try{
   if(path==="/api/register"&&request.method==="POST"){
    const {email,password,name,avatar}=await request.json();if(!email||!password||password.length<6||!name)return json({error:"ایمیل، نام و رمز حداقل ۶ کاراکتری لازم است."},400);
    const e=email.trim().toLowerCase();if(!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(e))return json({error:"ایمیل معتبر نیست."},400);
    const salt=crypto.randomUUID()+crypto.randomUUID(),hash=await pwHash(password,salt);
    try{await env.DB.prepare("INSERT INTO users(email,name,avatar,salt,password_hash,created_at) VALUES(?,?,?,?,?,?)").bind(e,name.trim(),avatar||"🚘",salt,hash,new Date().toISOString()).run()}catch{return json({error:"این ایمیل قبلاً ثبت شده است."},409)}
    const user={email:e,name:name.trim(),avatar:avatar||"🚘"};return json({user,token:await sign(user.email,env.JWT_SECRET)});
   }
   if(path==="/api/login"&&request.method==="POST"){
    const {email,password}=await request.json();const e=(email||"").trim().toLowerCase();const u=await env.DB.prepare("SELECT email,name,avatar,salt,password_hash FROM users WHERE email=?").bind(e).first();
    if(!u||await pwHash(password||"",u.salt)!==u.password_hash)return json({error:"ایمیل یا رمز عبور نادرست است."},401);
    const user={email:u.email,name:u.name,avatar:u.avatar};return json({user,token:await sign(user.email,env.JWT_SECRET)});
   }
   const email=await auth(request,env.JWT_SECRET);if(!email)return json({error:"نیاز به ورود مجدد است."},401);
   if(path==="/api/profile"&&request.method==="PUT"){const {name,avatar}=await request.json();if(!name?.trim())return json({error:"نام لازم است."},400);await env.DB.prepare("UPDATE users SET name=?,avatar=? WHERE email=?").bind(name.trim(),avatar||"🚘",email).run();return json({user:{email,name:name.trim(),avatar:avatar||"🚘"}})}
   if(path==="/api/data"&&request.method==="GET"){const row=await env.DB.prepare("SELECT data_json FROM user_data WHERE email=?").bind(email).first();return json({data:row?JSON.parse(row.data_json):null})}
   if(path==="/api/data"&&request.method==="PUT"){const {data}=await request.json();if(!data||typeof data!=="object")return json({error:"داده نامعتبر است."},400);await env.DB.prepare("INSERT INTO user_data(email,data_json,updated_at) VALUES(?,?,?) ON CONFLICT(email) DO UPDATE SET data_json=excluded.data_json,updated_at=excluded.updated_at").bind(email,JSON.stringify(data),new Date().toISOString()).run();return json({ok:true})}
   return json({error:"مسیر یافت نشد."},404)
  }catch(e){return json({error:"خطای سرور: "+(e?.message||"")},500)}
 }
};
async function pwHash(p,s){const k=await crypto.subtle.importKey("raw",new TextEncoder().encode(p),"PBKDF2",false,["deriveBits"]);const b=await crypto.subtle.deriveBits({name:"PBKDF2",salt:new TextEncoder().encode(s),iterations:160000,hash:"SHA-256"},k,256);return btoa(String.fromCharCode(...new Uint8Array(b)))}
function b64u(b){return btoa(String.fromCharCode(...new Uint8Array(b))).replace(/=/g,"").replace(/\+/g,"-").replace(/\//g,"_")}
async function sign(email,secret){const h=b64u(new TextEncoder().encode(JSON.stringify({alg:"HS256",typ:"JWT"}))),p=b64u(new TextEncoder().encode(JSON.stringify({sub:email,exp:Math.floor(Date.now()/1000)+60*60*24*30}))),key=await crypto.subtle.importKey("raw",new TextEncoder().encode(secret),{name:"HMAC",hash:"SHA-256"},false,["sign"]),sig=b64u(await crypto.subtle.sign("HMAC",key,new TextEncoder().encode(h+"."+p)));return h+"."+p+"."+sig}
async function auth(req,secret){try{const t=req.headers.get("Authorization")?.replace(/^Bearer\s+/,"");if(!t)return null;const [h,p,s]=t.split("."),k=await crypto.subtle.importKey("raw",new TextEncoder().encode(secret),{name:"HMAC",hash:"SHA-256"},false,["verify"]),raw=Uint8Array.from(atob(s.replace(/-/g,"+").replace(/_/g,"/")),c=>c.charCodeAt(0));if(!await crypto.subtle.verify("HMAC",k,raw,new TextEncoder().encode(h+"."+p)))return null;const v=JSON.parse(atob(p.replace(/-/g,"+").replace(/_/g,"/")));return v.exp>Date.now()/1000?v.sub:null}catch{return null}}
