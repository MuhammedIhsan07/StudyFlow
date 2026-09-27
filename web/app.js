'use strict';

const state = { me: null, csrf: '', users: [], planner: null, page: 'overview', selected: null,
  detailTab: 'overview', query: '', filter: 'all', week: 0, today: '', setup: false, loading: false };
const root = document.querySelector('#app');
const dialog = document.querySelector('#dialog');
let dialogSubmit = null, loadSequence = 0, toastTimer;
const paths = {
  book: '<path d="M3 4c4-1 7 0 9 2 2-2 5-3 9-2v15c-4-1-7 0-9 2-2-2-5-3-9-2z"/><path d="M12 6v15"/>',
  grid: '<rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/>',
  calendar: '<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M16 3v4M8 3v4M3 11h18M8 15h1M15 15h1"/>',
  tasks: '<rect x="5" y="4" width="15" height="17" rx="2"/><path d="M9 3h7v4H9zM9 12l1 1 2-2M14 12h3M9 17h8"/>',
  chart: '<path d="M4 3v17h17M8 15v-4M13 15V7M18 15v-6"/>',
  users: '<path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75"/><circle cx="9" cy="7" r="4"/>',
  mentor: '<path d="m2 8 10-5 10 5-10 5zM6 10v6c4 3 8 3 12 0v-6M22 8v7"/>',
  settings: '<path d="m9 3-1 3-3 1 1 3-2 2 2 2-1 3 3 1 1 3h6l1-3 3-1-1-3 2-2-2-2 1-3-3-1-1-3z"/><circle cx="12" cy="12" r="3"/>',
  arrow: '<path d="M5 12h14m-5-5 5 5-5 5"/>',
  left: '<path d="m15 5-7 7 7 7"/>', right: '<path d="m9 5 7 7-7 7"/>',
  plus: '<path d="M12 5v14M5 12h14"/>', check: '<path d="m5 12 4 4L19 6"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>',
  target: '<circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="5"/><circle cx="12" cy="12" r="1"/>',
  spark: '<path d="m12 3 2.5 6.5L21 12l-6.5 2.5L12 21l-2.5-6.5L3 12l6.5-2.5zM20 2v4M18 4h4"/>',
  leaf: '<path d="M20 3C10 1 2 7 5 15s15 4 15-12zM4 21 15 9M7 16l-1-6M11 12l6 1"/>',
  logout: '<path d="M9 4H4v16h5M9 12h12m-5-5 5 5-5 5"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  edit: '<path d="m15 4 5 5M4 20l5-1L21 7a2 2 0 0 0-5-5L4 15z"/>',
  trash: '<path d="M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  refresh: '<path d="M20 7v5h-5M4 17v-5h5M5 7a8 8 0 0 1 13-3l2 3M4 17l2 3a8 8 0 0 0 13-3"/>',
  shield: '<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6zM8 12l3 3 5-6"/>',
  menu: '<path d="M4 6h16M4 12h16M4 18h16"/>',
  note: '<path d="M21 11v9H4V3h9M8 16l4-1 9-9-3-3-9 9z"/>',
  mail: '<rect x="3" y="5" width="18" height="14" rx="2"/><path d="m3 6 9 7 9-7"/>',
  copy: '<rect x="8" y="8" width="12" height="13" rx="2"/><path d="M16 8V3H3v13h5"/>',
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.book}</svg>`;
const h = value => String(value ?? '').replace(/[&<>"']/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[char]));
const initials = name => String(name).trim().split(/\s+/).map(part => [...part][0]).slice(0, 2).join('').toUpperCase();
const avatar = (name, extra = '') => `<span class="avatar ${extra}" aria-hidden="true">${h(initials(name))}</span>`;
const duration = minutes => minutes >= 60 ? `${Math.floor(minutes / 60)}h${minutes % 60 ? ` ${minutes % 60}m` : ''}` : `${minutes}m`;
const labelRole = role => ({ADMIN:'Administrator',MENTOR:'Mentor',STUDENT:'Student'}[role] || role);
const typeLabel = type => ({STUDY:'Study session',REVISION:'Revision',ASSIGNMENT:'Assignment',EXAM:'Exam preparation'}[type] || type);
const dayDate = date => new Date(`${date}T12:00:00`);
const dateString = date => `${date.getFullYear()}-${String(date.getMonth()+1).padStart(2,'0')}-${String(date.getDate()).padStart(2,'0')}`;
const addDays = (date, offset) => { const d = dayDate(date); d.setDate(d.getDate() + offset); return dateString(d); };
const formatDate = (date, options = {day:'numeric',month:'short'}) => dayDate(date).toLocaleDateString(undefined, options);
const timestamp = date => new Date(date).toLocaleString(undefined, {day:'numeric',month:'short',hour:'numeric',minute:'2-digit'});
const today = () => state.today || dateString(new Date());
const weekStart = date => {const d = dayDate(date); return addDays(date, -((d.getDay()+6)%7));};
const staff = () => state.me?.role !== 'STUDENT';
const admin = () => state.me?.role === 'ADMIN';
const studentPath = () => `/api/students/${state.selected || state.me.id}`;
const overdue = task => task.status !== 'COMPLETED' && (task.overdue ?? task.scheduled_date < today());

async function api(path, method = 'GET', data) {
  let response;
  try {
    response = await fetch(path, {method, credentials:'same-origin', headers:{...(method === 'GET' ? {} : {'Content-Type':'application/json','X-CSRF-Token':state.csrf})},
      ...(method === 'GET' ? {} : {body:JSON.stringify(data ?? {})})});
  } catch { throw new Error('Cannot reach StudyFlow. Check that the server is running, then try again.'); }
  const result = await response.json();
  if (!response.ok) {
    if (response.status === 401 && state.me && path !== '/api/auth/login') {
      state.me = null; state.csrf = ''; state.users = []; state.planner = null;
      dialog.close(); showAuth();
    }
    const error = new Error(result.error || 'Something went wrong. Please try again.');
    error.status = response.status;
    throw error;
  }
  return result;
}
function toast(message, error = false) {
  const element = document.querySelector('#toast');
  clearTimeout(toastTimer); element.textContent = message;
  element.className = `visible${error ? ' error' : ''}`;
  toastTimer = setTimeout(() => element.className = '', error ? 7500 : 4500);
}
function brand() { return `<div class="brand"><span class="brand-mark">${icon('book')}</span>StudyFlow<span class="small muted"></span></div>`; }
function authStory() { return `<aside class="auth-story">${brand()}<h1>A little structure.<br>A lot more<br><em>possibility.</em></h1><p>A calmer place to plan your studies, find your rhythm, and grow with a little guidance.</p>
  <div class="mini-plan" aria-hidden="true"><div class="mini-plan-head"><h3>Your next chapter</h3>${icon('leaf')}</div><div class="mini-plan-line"><span class="mini-check">${icon('check')}</span><div>Make a little progress<small>One focused session at a time</small></div></div><div class="mini-plan-line"><span class="mini-check">${icon('book')}</span><div>Learn with intention<small>A plan that works around you</small></div></div><div class="mini-plan-line"><span class="mini-check">${icon('mentor')}</span><div>You don't have to do it alone<small>Your mentor is in your corner</small></div></div><div class="auth-badge">${icon('spark')} Small steps. Meaningful progress.</div></div>
  <div class="auth-story-foot">${icon('leaf')} Make room for what you're becoming.</div></aside>`; }
function showAuth() {
  const force = state.me?.must_change_password;
  document.title = `${force ? 'Set your password' : state.setup ? 'Welcome' : 'Sign in'} · StudyFlow`;
  root.innerHTML = `<div class="auth-layout">${authStory()}<main id="main" class="auth-content"><form class="auth-form" data-form="${force ? 'first-password' : state.setup ? 'setup' : 'login'}">
    <div class="eyebrow" style="margin-bottom:15px">${force ? 'YOUR ACCOUNT, YOUR PASSWORD' : state.setup ? 'A FRESH START' : 'WELCOME BACK'}</div>
    <h2>${force ? 'Make yourself at home.' : state.setup ? 'Your learning community starts here.' : 'Good to see you again.'}</h2>
    <p class="muted small">${force ? 'Set a personal password to finish activating your account.' : state.setup ? 'Create the first administrator account. You can add students and mentors next.' : 'Sign in to your StudyFlow workspace.'}</p>
    ${state.setup && !force ? `${field('Setup key','setup_token','password','Paste the key from the server terminal',{autocomplete:'off',required:true})}${field('Your name','name','text','Your full name',{maxlength:100,autocomplete:'name',required:true})}` : ''}
    ${force ? field('Temporary password','current_password','password','Your temporary password',{autocomplete:'current-password',required:true,maxlength:128}) : field('Email address','email','email','you@school.edu',{autocomplete:'username',required:true,maxlength:254})}
    ${field(force ? 'New password' : state.setup ? 'Choose a password' : 'Password','password','password',force || state.setup ? 'At least 12 characters' : 'Enter your password',{autocomplete:force || state.setup ? 'new-password' : 'current-password',required:true,minlength:force || state.setup ? 12 : 1,maxlength:128})}
    ${force || state.setup ? field('Confirm password','confirm_password','password','Enter it again',{autocomplete:'new-password',required:true,minlength:12,maxlength:128}) : '<div style="text-align:right;margin-top:-5px;margin-bottom:10px"><button type="button" class="text-button" data-action="forgot">Forgot your password?</button></div>'}
    <div class="form-error" role="alert"></div><button class="button full" type="submit">${force ? 'Set password & continue' : state.setup ? 'Create my workspace' : 'Sign in'} ${icon('arrow')}</button>
    <div class="auth-help">${force ? '<button type="button" class="text-button" data-action="logout">Sign in with a different account</button>' : state.setup ? 'The setup key appears in the terminal where StudyFlow is running.' : 'New to StudyFlow? Ask your administrator or mentor<br>to create an account for you.'}</div></form></main></div>`;
}
function field(label, name, type='text', placeholder='', attrs={}) {
  return `<label class="field"><span>${h(label)}</span><input type="${type}" name="${name}" placeholder="${h(placeholder)}" ${Object.entries(attrs).map(([key,value]) => value === true ? key : value === false ? '' : `${key}="${h(value)}"`).join(' ')}></label>`;
}
function selectField(label, name, options, selected, extra='') {
  return `<label class="field"><span>${h(label)}</span><select name="${name}" ${extra}>${options.map(([value,text]) => `<option value="${h(value)}" ${value === selected ? 'selected' : ''}>${h(text)}</option>`).join('')}</select></label>`;
}
function empty(title, description, action='', buttonText='', symbol='leaf') {
  return `<div class="empty">${icon(symbol)}<h3>${h(title)}</h3><p>${h(description)}</p>${action ? `<button class="button soft small" data-action="${action}">${icon('plus')}${h(buttonText)}</button>` : ''}</div>`;
}
function metric(title, value, subtitle, symbol, accent=false) { return `<div class="metric"><div class="metric-top">${h(title)}${icon(symbol)}</div><div class="metric-value">${h(value)}</div><small class="${accent?'accent':''}">${h(subtitle)}</small></div>`; }
function panel(title, subtitle, content, action='', actionLabel='View all', extra='') {
  return `<section class="panel ${extra}"><div class="panel-header"><div><h2>${h(title)}</h2>${subtitle ? `<p>${h(subtitle)}</p>` : ''}</div>${action ? `<button class="text-button" data-action="${action}">${h(actionLabel)} ${icon('arrow')}</button>` : ''}</div><div class="panel-body">${content}</div></section>`;
}
function shell(content) {
  const navItems = staff() ? [['overview','grid','Overview'],['students','users','Students'],...(admin() ? [['mentors','mentor','Mentors'],['admins','shield','Administrators'],['activity','clock','Account activity']] : [])] : [['overview','grid','Overview'],['schedule','calendar','My schedule'],['subjects','book','Subjects'],['tasks','tasks','Tasks'],['progress','chart','My progress']];
  const pageName = state.selected ? 'Student workspace' : ({settings:'Settings',...Object.fromEntries(navItems.map(([id,,label])=>[id,label]))}[state.page] || 'Overview');
  document.title = `${pageName} · StudyFlow`;
  root.innerHTML = `<div class="shell"><aside class="sidebar">${brand()}<div class="workspace-label">${staff() ? 'YOUR COMMUNITY' : 'YOUR WORKSPACE'}</div><nav class="nav" aria-label="Main navigation">${navItems.map(([id,symbol,label])=>`<button class="nav-button ${state.page===id&&!state.selected?'active':''}" data-action="navigate" data-page="${id}" ${state.page===id&&!state.selected?'aria-current="page"':''}>${icon(symbol)}${label}</button>`).join('')}</nav><div class="sidebar-bottom"><div class="sidebar-card">${icon('leaf')}<p>Progress grows<br>one day at a time.</p><small>A little consistency goes a long way.</small></div><button class="nav-button ${state.page==='settings'?'active':''}" data-action="navigate" data-page="settings">${icon('settings')}Settings</button><div class="sidebar-user">${avatar(state.me.name)}<div class="identity"><strong>${h(state.me.name)}</strong><small>${labelRole(state.me.role)}</small></div><button class="icon-button" data-action="logout" aria-label="Sign out" title="Sign out">${icon('logout')}</button></div></div></aside>
    <div class="workspace"><header class="topbar"><button class="icon-button mobile-toggle" data-action="menu" aria-label="Open navigation" aria-expanded="false">${icon('menu')}</button><div class="breadcrumbs">${icon('grid')}Workspace ${icon('right')}<strong>${pageName}</strong></div><div class="topbar-right"><span class="date">${h(formatDate(today(),{weekday:'long',day:'numeric',month:'long',year:'numeric'}))}</span><button class="icon-button" data-action="refresh" aria-label="Refresh workspace" title="Refresh">${icon('refresh')}</button>${avatar(state.me.name,'sage')}</div></header><main id="main" class="main">${content}<footer class="footer"><span>${icon('leaf')} A little progress, every day.</span><span>StudyFlow · Your space to grow</span></footer></main></div></div>`;
}
async function navigate(page='overview', selected=null) {
  state.page = page; state.selected = selected; state.query = ''; state.filter = 'all'; state.week = 0; state.detailTab = 'overview';
  await refresh(); window.scrollTo(0,0);
}
async function refresh() {
  if (!state.me) return;
  const sequence = ++loadSequence;
  state.loading = true;
  try {
    let users = state.users, planner = state.planner, activity = state.activity;
    if (staff()) users = await api('/api/users');
    if (!staff() || state.selected) planner = await api(studentPath() + '/planner');
    if (admin() && state.page === 'activity') activity = await api('/api/audit');
    if (sequence !== loadSequence || !state.me) return;
    Object.assign(state,{users,planner,activity});
    if (planner?.today && (!staff() || state.selected)) state.today = planner.today;
    render();
  } catch (error) {
    if (state.me && sequence === loadSequence) shell(`${empty('We could not load this workspace.', error.message)}<div style="text-align:center"><button class="button" data-action="refresh">${icon('refresh')}Try again</button></div>`);
    throw error;
  } finally { state.loading = false; }
}
function render() {
  if (!state.me) return showAuth();
  if (state.me.must_change_password) return showAuth();
  let content;
  if (state.page === 'settings') content = settingsPage();
  else if (state.selected) content = studentDetail();
  else if (staff()) content = state.page==='overview' ? staffOverview() : state.page==='activity' ? activityPage() : peoplePage();
  else content = studentPage(state.page);
  shell(content);
}
function heading(title, subtitle, actions='') { return `<div class="page-heading"><div><h1>${h(title)}</h1><p>${h(subtitle)}</p></div>${actions?`<div class="heading-actions">${actions}</div>`:''}</div>`; }
function addButton(action, label) { return `<button class="button" data-action="${action}">${icon('plus')}${h(label)}</button>`; }

function staffOverview() {
  const students = state.users.filter(user=>user.role==='STUDENT');
  const active = students.filter(user=>user.active);
  const mentors = state.users.filter(user=>user.role==='MENTOR' && user.active);
  const needsHelp = active.filter(user=>user.stats.overdue > 0);
  const completed = active.reduce((sum,user)=>sum+user.stats.completed,0);
  const unassigned = active.filter(user=>!user.mentor_id);
  const firstName = state.me.name.split(' ')[0];
  return heading(`Welcome back, ${firstName}.`, 'A little clarity for you. A little more support for your students.',`${admin()?'<button class="button secondary" data-action="add-mentor">'+icon('plus')+'Add mentor</button>':''}${addButton('add-student','Add student')}`)
    + `<section class="welcome-banner"><div><span class="eyebrow">LEARNING IS BETTER TOGETHER</span><h2>${students.length ? 'Every student has a next chapter.' : "Let's make space for your first students."}</h2><p>${students.length ? 'Keep an eye on progress, celebrate the small wins, and step in where a little guidance can make a difference.' : 'Add a student, connect them with a mentor, and help turn good intentions into a plan.'}</p></div><div class="banner-art">${icon('book')}</div></section>`
    + `<div class="metrics">${metric('Active students',active.length,'In your learning community','users')}${metric(admin()?'Active mentors':'Tasks completed',admin()?mentors.length:completed,admin()?'Here to guide and encourage':'Across your students',admin()?'mentor':'check')}${metric('Need a check-in',needsHelp.length,'Students with overdue sessions','clock')}${metric(admin()?'Ready for a mentor':'Study time this week',admin()?unassigned.length:duration(active.reduce((n,u)=>n+u.stats.weekly_minutes,0)),admin()?'Students waiting for an assignment':'Logged through completed sessions',admin()?'leaf':'chart')}</div>`
    + `<div class="grid-main"><div class="stack">${panel('Your students','Real people. Individual journeys.', students.length ? students.slice(0,5).map(studentPreview).join('') : empty('Your community starts with one student.','Create an account and share their temporary password to get started.','add-student','Add your first student','users'),'all-students','View students')}
    ${panel('A little attention goes a long way', 'Students who may benefit from a check-in', needsHelp.length ? needsHelp.slice(0,4).map(studentPreview).join('') : empty('Room to breathe.','No active students have overdue sessions right now.','','','leaf'))}</div><div class="stack">${panel('Build a learning community','A simple path from setup to support',`<div class="flow-steps"><div class="flow-step"><span>1</span>Add students and share their sign-in details.</div><div class="flow-step"><span>2</span>${admin()?'Add mentors and connect them with students.':'New students you add join your own cohort.'}</div><div class="flow-step"><span>3</span>Make a plan with subjects and focused sessions.</div><div class="flow-step"><span>4</span>Track progress and leave thoughtful feedback.</div></div><div class="notice">${admin()?'Students see their own planner. Mentors see only the students assigned to them. You manage the community.':'Your workspace includes only your assigned students. Ask an administrator to transfer students between mentors.'}</div>`)}${panel('Progress, with perspective','What your numbers mean',`<p class="small muted">Study time comes from completed sessions, and weekly goals reset each Monday. A missed session is an invitation to adjust the plan.</p><p class="small muted" style="margin:0">A new student starts with a clean slate. Add their first subject to give their planner some direction.</p>`)}</div></div>`;
}
function studentPreview(user) {
  return `<div class="task-row">${avatar(user.name,'sage')}<div class="task-copy"><div class="task-title">${h(user.name)}</div><div class="task-meta">${h(user.program || 'No program added')}<span>·</span>${user.stats.completed} / ${user.stats.total} tasks complete</div></div><div class="task-trailing">${user.stats.overdue ? `<span class="tag warning">${user.stats.overdue} overdue</span>` : `<span class="tag neutral">${user.stats.total ? 'In progress' : 'Getting started'}</span>`}<button class="icon-button" data-action="view-student" data-id="${user.id}" aria-label="Open ${h(user.name)}'s planner">${icon('arrow')}</button></div></div>`;
}
function peoplePage() {
  const role = {students:'STUDENT',mentors:'MENTOR',admins:'ADMIN'}[state.page] || 'STUDENT';
  const title = {STUDENT:'Your students',MENTOR:'Your mentors',ADMIN:'Administrators'}[role];
  const all = state.users.filter(user=>user.role===role);
  const users = all.filter(user=>(state.filter==='all'||state.filter==='active'&&user.active||state.filter==='inactive'&&!user.active||state.filter==='unassigned'&&!user.mentor_id)&&`${user.name} ${user.email} ${user.program}`.toLowerCase().includes(state.query.toLowerCase()));
  return heading(title, role==='STUDENT'?'Every learner, with room to grow.':role==='MENTOR'?'The people helping your students find their way.':'Manage who can look after your learning community.',addButton(role==='STUDENT'?'add-student':role==='MENTOR'?'add-mentor':'add-admin',role==='STUDENT'?'Add student':role==='MENTOR'?'Add mentor':'Add administrator'))
    + `<div class="toolbar"><div class="search-wrap">${icon('search')}<input class="search-input" type="search" data-search="people" value="${h(state.query)}" placeholder="Search by name, email, or program…" aria-label="Search accounts"></div><div class="filters">${[['all','All'],['active','Active'],['inactive','Inactive'],...(role==='STUDENT'&&admin()?[['unassigned','Unassigned']]:[])].map(([id,label])=>`<button class="filter ${state.filter===id?'active':''}" data-action="filter" data-filter="${id}">${label}</button>`).join('')}</div></div>
    <div id="people-results">${users.length?`<div class="people-grid">${users.map(personCard).join('')}</div>`:empty(all.length?'No matching accounts.':`Your first ${role.toLowerCase()} belongs here.`,all.length?'Try another search or filter.':'Create an account to start building your learning community.',all.length?'':role==='STUDENT'?'add-student':role==='MENTOR'?'add-mentor':'add-admin','Add account','users')}</div>`;
}
function personCard(user) {
  const mentor = state.users.find(account=>account.id===user.mentor_id);
  const cohort = state.users.filter(account=>account.mentor_id===user.id).length;
  return `<article class="person-card"><div class="person-top">${avatar(user.name,'sage')}<span class="tag ${!user.active?'neutral':user.must_change_password?'warning':''}"><span class="dot"></span>${!user.active?'Inactive':user.must_change_password?'First login pending':'Active'}</span></div><h3>${h(user.name)}</h3><div class="person-email">${h(user.email)}</div><div class="person-program">${h(user.program || labelRole(user.role))}</div>
    ${user.role==='STUDENT'?`<div class="person-stats"><div><strong>${user.stats.completion_rate}%</strong><small>Tasks complete</small></div><div><strong>${duration(user.stats.weekly_minutes)}</strong><small>Study this week</small></div></div><div class="person-assignment">${icon('mentor')} ${h(mentor?mentor.name+(mentor.active?'':' (inactive)'):user.mentor_id?state.me.name:'No mentor assigned')}</div>`:user.role==='MENTOR'?`<div class="person-stats"><div><strong>${cohort}</strong><small>Assigned students</small></div></div>`:''}
    <div class="person-actions">${user.role==='STUDENT'?`<button class="button secondary small" data-action="view-student" data-id="${user.id}">Open planner ${icon('arrow')}</button>`:''}${admin()?`<button class="${user.role==='STUDENT'?'icon-button':'button secondary small'}" data-action="edit-user" data-id="${user.id}" aria-label="Manage ${h(user.name)}" title="Manage account">${icon('settings')}${user.role==='STUDENT'?'':'Manage account'}</button>`:''}</div></article>`;
}
function activityPage() {
  const actions = {'workspace.created':'Created workspace','account.created':'Added account','account.updated':'Updated account','password.reset':'Reset password','password.changed':'Changed own password'};
  return heading('Account activity','A record of account creation, changes, and password resets.') + `<section class="panel">${state.activity?.length?`<div class="table-wrap"><table><thead><tr><th>Who</th><th>Action</th><th>Account</th><th>When</th></tr></thead><tbody>${state.activity.map(event=>`<tr><td>${h(event.actor_name)}</td><td>${h(actions[event.action]||event.action)}</td><td>${h(event.target_name)}</td><td>${h(timestamp(event.created_at))}</td></tr>`).join('')}</tbody></table></div>`:empty('Nothing to show yet.','Account changes will appear here.','','','clock')}</section>`;
}

function studentPage(page, embedded=false) {
  const titles = {overview:[`A little progress, ${state.planner.student.name.split(' ')[0]}.`,'A fresh page. A clear plan. You’ve got this.'],tasks:['Your study sessions','Make space for the work that matters.'],subjects:['Your subjects','A home for every part of your learning.'],schedule:['Your week, at a glance','Find your rhythm, one focused session at a time.'],progress:['Look how far you’ve come.','The small steps add up. Here’s the evidence.']};
  const title = titles[page] || titles.overview;
  const actions = page==='subjects' ? addButton('add-subject','Add subject') : page==='progress' ? '' : addButton('add-task','New study session');
  const head = embedded ? `<div class="toolbar"><div class="muted small">${h(title[1])}</div>${actions}</div>` : heading(title[0],title[1],actions);
  if (page==='subjects') return head+subjectsPage();
  if (page==='tasks') return head+tasksPage();
  if (page==='schedule') return head+schedulePage();
  if (page==='progress') return head+progressPage();
  return head+studentOverview(embedded);
}
function studentMetrics() {
  const stats=state.planner.stats;
  return `<div class="metrics">${metric('Study time this week',duration(stats.weekly_minutes),'Time invested in your learning','clock',true)}${metric('Tasks completed',`${stats.completed} / ${stats.total}`,stats.total?`${stats.completion_rate}% of your plan complete`:'A fresh start awaits','check')}${metric('Subjects',state.planner.subjects.length,'Different paths. One bigger picture.','book')}${metric('Today’s plan',duration(stats.today_planned),`${duration(state.planner.student.daily_goal)} daily study goal`,'target')}</div>`;
}
function studentOverview(embedded=false) {
  const {tasks,subjects,stats,mentor}=state.planner;
  const recommended = tasks.filter(task=>task.status==='SCHEDULED').sort((a,b)=>b.urgency-a.urgency)[0];
  const daily = tasks.filter(task=>task.scheduled_date===today());
  const next = tasks.filter(task=>task.status==='SCHEDULED'&&task.scheduled_date>today()).slice(0,3);
  return `${!embedded?`<section class="welcome-banner"><div><span class="eyebrow">${recommended?'A LITTLE FOCUS GOES A LONG WAY':'YOUR NEXT CHAPTER STARTS HERE'}</span><h2>${h(recommended?recommended.title:'Big goals begin with small steps.')}</h2><p>${recommended?`${h(recommended.subject_name)} · ${duration(recommended.duration)} of focused learning. ${overdue(recommended)?'This session needs a new time. Open Tasks to adjust it.':'One session is a great place to start.'}`:'Add your first subject, plan a study session, and make a little room for progress.'}</p></div><div class="banner-art">${icon(recommended?'leaf':'book')}</div></section>`:''}
    ${studentMetrics()}${stats.overdue?`<div class="overdue-banner">${icon('clock')}<span>${stats.overdue} ${stats.overdue===1?'session needs':'sessions need'} a fresh start. Open Tasks to reschedule around your daily goal.</span></div>`:''}
    <div class="grid-main"><div class="stack">${panel('On the page today',formatDate(today(),{weekday:'long',day:'numeric',month:'long'}),daily.length?daily.map(taskRow).join(''):empty('A little breathing room.','Your schedule is clear today. Add a session when you’re ready.','add-task','Plan a session','calendar'),'go-tasks','All tasks')}
    ${panel('Coming up next','A little preparation goes a long way',next.length?next.map(taskRow).join(''):empty('The next chapter is yours.','Your upcoming sessions will appear here.','','','calendar'),'go-schedule','See schedule')}
    ${mentor?`<div class="mentor-strip">${avatar(mentor.name,'sage')}<div><strong>${h(mentor.name)}</strong><small>${mentor.active?'Your mentor · Here to help you grow':'Your mentor is currently inactive · Contact your administrator'}</small></div></div>`:!embedded?'<div class="notice">Your administrator can connect you with a mentor. In the meantime, your planner is ready to use.</div>':''}</div>
    <div class="stack">${panel('Your study rhythm','Completed study time · This week',weeklyChart())}${panel('Little steps, by subject','Progress toward your weekly study goals',subjects.length?subjectProgress():empty('Give your learning a home.','Add a subject and choose a weekly goal.','add-subject','Add a subject','book'),'go-subjects','All subjects')}${panel('A word from your mentor','Feedback to help you find your way',notesContent(),staff()?'add-note':'', 'Add note')}</div></div>`;
}
function taskRow(task) {
  const done=task.status==='COMPLETED';
  return `<article class="task-row"><button class="task-check ${done?'done':''}" data-action="complete-task" data-id="${task.id}" aria-label="${done?'Reopen':'Complete'} ${h(task.title)}" aria-pressed="${done}">${done?icon('check'):''}</button><div class="task-copy"><div class="task-title ${done?'done':''}">${h(task.title)}</div><div class="task-meta"><span class="subject-label"><span class="dot" style="color:${h(task.subject_color)}"></span>${h(task.subject_code)}</span><span>${task.scheduled_date===today()?'Today':h(formatDate(task.scheduled_date))}, ${h(task.start_time)}</span><span>·</span><span>${duration(task.duration)}</span>${task.deadline?`<span class="task-deadline">Due ${h(formatDate(task.deadline))}</span>`:''}</div></div><div class="task-trailing">${done?'<span class="tag neutral">Done</span>':overdue(task)?'<span class="tag warning">Overdue</span>':task.priority==='HIGH'?'<span class="tag warning">High priority</span>':''}${!done&&task.adaptive?`<button class="icon-button" data-action="reschedule-task" data-id="${task.id}" title="Find a new study slot" aria-label="Reschedule ${h(task.title)}">${icon('spark')}</button>`:''}<button class="icon-button" data-action="edit-task" data-id="${task.id}" title="Edit session" aria-label="Edit ${h(task.title)}">${icon('edit')}</button></div></article>`;
}
function weeklyChart() {
  const stats=state.planner.stats, max=Math.max(60,...stats.daily_minutes);
  return `<div class="chart" role="img" aria-label="Study time this week: ${h(stats.daily_minutes.map((minutes,i)=>`${['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'][i]} ${minutes} minutes`).join(', '))}">${stats.daily_minutes.map((minutes,i)=>`<div class="chart-column ${addDays(stats.week_start,i)===today()?'today':''}"><span class="chart-value">${minutes?duration(minutes):''}</span><div class="chart-bar" style="height:${Math.max(3,Math.round(minutes/max*92))}px"></div><small>${['M','T','W','T','F','S','S'][i]}</small></div>`).join('')}</div><div class="chart-foot"><span class="dot"></span>Completed sessions · ${duration(stats.weekly_minutes)} this week</div>`;
}
function subjectProgress() {
  return state.planner.subjects.map(subject=>`<div class="subject-row"><div class="subject-row-top"><strong>${h(subject.name)}</strong><span>${Math.round(subject.weekly_minutes/subject.weekly_goal*100)}%</span></div><progress class="progress" style="--progress-color:${h(subject.color)}" value="${Math.min(subject.weekly_minutes,subject.weekly_goal)}" max="${subject.weekly_goal}" aria-label="${h(subject.name)} weekly goal"></progress><small>${duration(subject.weekly_minutes)} of ${duration(subject.weekly_goal)} weekly goal</small></div>`).join('');
}
function notesContent() {
  return state.planner.notes.length?state.planner.notes.map(note=>`<div class="note-card"><div class="note-meta"><strong>${h(note.author_name)}</strong><span>${h(timestamp(note.created_at))}</span></div><p>${h(note.body)}</p><div class="note-meta"><span class="tag ${note.shared?'':'neutral'}">${note.shared?'Shared with student':'Staff only'}</span>${staff()&&(admin()||note.author_id===state.me.id)?`<button class="icon-button" data-action="delete-note" data-id="${note.id}" aria-label="Delete note">${icon('trash')}</button>`:''}</div></div>`).join(''):empty('A conversation can start here.',staff()?'Share encouragement or keep a private staff note.':'Your mentor’s feedback will appear here.','','','note');
}
function subjectsPage() {
  return state.planner.subjects.length?`<div class="people-grid">${state.planner.subjects.map(subject=>`<article class="panel subject-card"><div class="subject-card-top"><span class="subject-icon" style="color:${h(subject.color)}">${icon('book')}</span><button class="icon-button" data-action="edit-subject" data-id="${subject.id}" aria-label="Edit ${h(subject.name)}">${icon('edit')}</button></div><h3>${h(subject.name)}</h3><span class="subject-code">${h(subject.code)}</span><div class="subject-goal"><span>${duration(subject.weekly_minutes)} / ${duration(subject.weekly_goal)} this week</span><span>${Math.round(subject.weekly_minutes/subject.weekly_goal*100)}%</span></div><progress class="progress" value="${Math.min(subject.weekly_minutes,subject.weekly_goal)}" max="${subject.weekly_goal}" style="--progress-color:${h(subject.color)}" aria-label="${h(subject.name)} weekly goal"></progress><div class="subject-actions"><button class="text-button" data-action="add-task" data-subject="${subject.id}">${icon('plus')}Plan a session</button><button class="icon-button" data-action="delete-subject" data-id="${subject.id}" aria-label="Delete ${h(subject.name)}">${icon('trash')}</button></div></article>`).join('')}</div>`:panel('Every subject is a new possibility.','',empty('What are you learning?','Add a subject and set a realistic weekly study goal.','add-subject','Add your first subject','book'));
}
function tasksPage() {
  const tasks=state.planner.tasks.filter(task=>(state.filter==='all'||state.filter==='open'&&task.status==='SCHEDULED'||state.filter==='completed'&&task.status==='COMPLETED'||state.filter==='overdue'&&overdue(task))&&`${task.title} ${task.subject_name}`.toLowerCase().includes(state.query.toLowerCase()));
  return `<div class="toolbar"><div class="search-wrap">${icon('search')}<input class="search-input" type="search" data-search="tasks" value="${h(state.query)}" placeholder="Find a session or subject…" aria-label="Search tasks"></div><div class="filters">${[['all','All sessions'],['open','To do'],['completed','Completed'],['overdue','Overdue']].map(([id,label])=>`<button class="filter ${state.filter===id?'active':''}" data-action="filter" data-filter="${id}">${label}</button>`).join('')}</div></div><div id="task-results"><section class="panel task-list">${tasks.length?tasks.map(taskRow).join(''):empty('A fresh page for your next session.',state.query||state.filter!=='all'?'Try another search or filter.':'Create a session and take your first small step.','add-task','Plan a session','tasks')}</section></div>`;
}
function schedulePage() {
  const start=addDays(weekStart(today()),state.week*7), end=addDays(start,6);
  return `<div class="calendar-toolbar"><button class="icon-button" data-action="week-prev" aria-label="Previous week">${icon('left')}</button><strong>${h(formatDate(start))} – ${h(formatDate(end,{day:'numeric',month:'short',year:'numeric'}))}</strong><button class="icon-button" data-action="week-next" aria-label="Next week">${icon('right')}</button><button class="button secondary small" data-action="week-today">This week</button></div><div class="calendar">${Array.from({length:7},(_,i)=>{const day=addDays(start,i),tasks=state.planner.tasks.filter(task=>task.scheduled_date===day);return `<section class="calendar-day ${day===today()?'today':''}" aria-label="${h(formatDate(day,{weekday:'long',day:'numeric',month:'long'}))}"><div class="day-heading"><span>${h(formatDate(day,{weekday:'short'}))}</span><strong>${dayDate(day).getDate()}</strong></div>${tasks.length?tasks.map(task=>`<button class="calendar-task ${task.status==='COMPLETED'?'done':''}" style="border-left-color:${h(task.subject_color)}" data-action="edit-task" data-id="${task.id}"><small>${h(task.start_time)} · ${duration(task.duration)}</small><strong>${h(task.title)}</strong><small>${h(task.subject_code)}${task.status==='COMPLETED'?' · Done':''}</small></button>`).join(''):'<div class="calendar-empty">Room to breathe</div>'}</section>`}).join('')}</div><p class="calendar-caption">${icon('spark')} Adaptive scheduling finds a free slot within your daily goal and keeps the original deadline.</p>`;
}
function progressPage() {
  return studentMetrics()+`<div class="grid-equal">${panel('Your study rhythm','Time invested in your learning this week',weeklyChart())}${panel('A little progress in every subject','Based on sessions completed this week',state.planner.subjects.length?subjectProgress():empty('Start with a subject.','Your progress will grow here as you complete sessions.','add-subject','Add subject','chart'))}</div><div style="margin-top:24px">${panel('Notes & encouragement','A place for thoughtful guidance',notesContent(),staff()?'add-note':'','Add note')}</div><div class="notice" style="margin-top:24px">Weekly study goals run Monday through Sunday. Completed time is recorded on the day you finish a task, even if you planned it for a different date. These totals describe study activity, not grades or mastery.</div>`;
}
function studentDetail() {
  const student=state.planner.student;
  return `<button class="text-button back-button" data-action="all-students">${icon('left')}Back to students</button><div class="page-heading"><div class="student-identity">${avatar(student.name,'large sage')}<div><h1>${h(student.name)}</h1><p>${h(student.program || 'Student workspace')} · ${h(student.email)}</p></div></div><div class="heading-actions">${admin()?`<button class="button secondary" data-action="edit-user" data-id="${student.id}">${icon('settings')}Manage account</button>`:''}<button class="button" data-action="add-note">${icon('note')}Add note</button></div></div>${!student.active?'<div class="overdue-banner">This account is inactive. An administrator must reactivate it before its planner can be changed.</div>':''}<nav class="detail-tabs" aria-label="Student workspace">${[['overview','Overview'],['tasks','Tasks'],['subjects','Subjects'],['schedule','Schedule'],['progress','Progress & notes']].map(([page,label])=>`<button class="detail-tab ${state.detailTab===page?'active':''}" data-action="detail-tab" data-tab="${page}">${label}</button>`).join('')}</nav>${studentPage(state.detailTab,true)}`;
}
function settingsPage() {
  const me=state.me;
  return heading('A workspace that feels like you.','Update your details, study preferences, and password.')+`<div class="settings-grid"><section class="panel"><div class="panel-header"><div><h2>Your profile</h2><p>A few details to make this space your own.</p></div>${avatar(me.name,'sage')}</div><form class="settings-form" data-form="profile">${field('Full name','name','text','',{value:me.name,required:true,maxlength:100})}${field('Email address','email','email','',{value:me.email,readonly:true})}<p class="small muted">Ask your administrator to change your sign-in email.</p>${field('Program / department','program','text','e.g. BSc Computer Science',{value:me.program,maxlength:120})}<div class="field-grid">${field('Daily study goal (minutes)','daily_goal','number','',{value:me.daily_goal,min:15,max:720,required:true})}${selectField('Preferred study time','preferred_time',[['MORNING','Morning'],['AFTERNOON','Afternoon'],['EVENING','Evening']],me.preferred_time)}</div><div class="form-error" role="alert"></div><div class="form-actions"><button class="button" type="submit">Save changes</button></div></form></section><section class="panel"><div class="panel-header"><div><h2>A password that’s yours</h2><p>Keep your learning space personal.</p></div>${icon('shield')}</div><form class="settings-form" data-form="password">${field('Current password','current_password','password','Your current password',{autocomplete:'current-password',required:true,maxlength:128})}${field('New password','password','password','At least 12 characters',{autocomplete:'new-password',required:true,minlength:12,maxlength:128})}${field('Confirm new password','confirm_password','password','Enter it again',{autocomplete:'new-password',required:true,minlength:12,maxlength:128})}<div class="notice">Changing your password signs you out on your other devices.</div><div class="form-error" role="alert"></div><div class="form-actions"><button class="button" type="submit">Update password</button></div></form></section></div>`;
}

function openDialog(title, description, content, submit=null, submitLabel='Save changes', extraActions='') {
  if (dialog.open) dialog.close();
  dialogSubmit = submit;
  dialog.innerHTML = `<div class="dialog-heading"><div><h2 id="dialog-title">${h(title)}</h2>${description?`<p>${h(description)}</p>`:''}</div><button class="icon-button" data-action="close-dialog" aria-label="Close dialog">${icon('close')}</button></div>${submit?'<form class="dialog-form" data-form="dialog">':'<div class="dialog-body">'}${content}${submit?`<div class="form-error" role="alert"></div><div class="form-actions">${extraActions}<button class="button secondary" type="button" data-action="close-dialog">Cancel</button><button class="button" type="submit">${h(submitLabel)}</button></div></form>`:'</div>'}`;
  dialog.showModal();
}
function confirmAction(title, description, callback, label='Confirm') {
  openDialog(title,description,'',async()=>{await callback();dialog.close();await refresh();},label);
}
function accountDialog(role='STUDENT', user=null) {
  const mentorOptions = [['','Not assigned yet'],...state.users.filter(account=>account.role==='MENTOR'&&account.active).map(account=>[account.id,account.name])];
  const mentorInactive = user?.mentor_id && !mentorOptions.some(([id])=>id===user.mentor_id);
  if (mentorInactive) mentorOptions.push([user.mentor_id, `${state.users.find(account=>account.id===user.mentor_id)?.name||'Previous mentor'} (inactive — reassign)`]);
  openDialog(user?'Manage account':`Add ${role==='ADMIN'?'an administrator':role==='MENTOR'?'a mentor':'a student'}`,
    user?'Update details, manage access, or reset a password.':'A small first step into your learning community.',
    `<div class="form-stack">${field('Full name','name','text','Full name',{value:user?.name||'',required:true,maxlength:100,autocomplete:'off'})}${field('Email address','email','email','you@school.edu',{value:user?.email||'',required:true,maxlength:254,autocomplete:'off'})}${field('Program / department','program','text','e.g. BSc Computer Science',{value:user?.program||'',maxlength:120})}
    ${role==='STUDENT'&&admin()?selectField('Assigned mentor','mentor_id',mentorOptions,user?.mentor_id||''):role==='STUDENT'?'<div class="notice">This student will be assigned to you.</div>':''}
    ${user?`<label class="check-field"><input name="active" type="checkbox" ${user.active?'checked':''} ${user.id===state.me.id?'disabled':''}> Account is active</label>${user.id!==state.me.id?`<div class="notice">Resetting a password ends all of this account’s sessions and creates a new temporary password.<br><button type="button" class="text-button" style="margin-top:8px" data-action="reset-password" data-id="${user.id}">${icon('refresh')}Reset password</button></div>`:''}`:'<div class="notice">We’ll generate a temporary password for you to share. The account holder must change it at first sign-in. No email is sent automatically.</div>'}</div>`,
    async data=>{
      const values={name:data.get('name'),email:data.get('email'),program:data.get('program'),mentor_id:data.get('mentor_id')||null};
      if (user) {
        values.active=user.id===state.me.id||data.has('active');
        const result=await api(`/api/users/${user.id}`,'PUT',values);
        if(user.id===state.me.id)state.me=result;
        dialog.close(); await refresh(); toast('Account updated.');
      } else {
        const result=await api('/api/users','POST',{...values,role});
        dialog.close(); await refresh(); credentialsDialog(result);
      }
    },user?'Save account':'Create account');
}
let credentialText='';
function credentialsDialog(result) {
  const {user,temporary_password:password}=result;
  credentialText=`StudyFlow sign-in\nURL: ${location.origin}\nEmail: ${user.email}\nTemporary password: ${password}\nYou will be asked to choose your own password at first sign-in.`;
  openDialog('Their next chapter is ready.',`Share these sign-in details with ${user.name}.`,
    `<div class="credentials"><label>Email address</label><code>${h(user.email)}</code><label>Temporary password</label><code>${h(password)}</code></div><p class="small muted">This password is shown only now. Share it privately with the account holder. They’ll choose their own password when they sign in.</p><div class="form-actions"><button class="button secondary" data-action="close-dialog">Done</button><button class="button" data-action="copy-credentials">${icon('copy')}Copy sign-in details</button></div><div class="copy-feedback" role="status"></div>`);
}
function subjectDialog(subject=null) {
  const colors=['#819768','#bca16b','#87a5a8','#a591b3','#bf8c78','#728497'];
  openDialog(subject?'A little fine-tuning.':'Give your learning a home.',subject?'Update this subject and its weekly goal.':'Start with a subject and a goal that feels achievable.',
    `<div class="form-stack">${field('Subject name','name','text','e.g. Object-Oriented Programming',{value:subject?.name||'',required:true,maxlength:100})}<div class="field-grid">${field('Subject code','code','text','e.g. CS201',{value:subject?.code||'',required:true,maxlength:20})}${field('Weekly goal (minutes)','weekly_goal','number','',{value:subject?.weekly_goal||180,min:15,max:3000,required:true})}</div>${selectField('Subject color','color',colors.map((color,i)=>[color,['Sage green','Warm sand','Soft teal','Lavender','Terracotta','Slate blue'][i]]),subject?.color||colors[0])}</div>`,
    async data=>{await api(studentPath()+'/subjects'+(subject?`/${subject.id}`:''),subject?'PUT':'POST',{name:data.get('name'),code:data.get('code'),weekly_goal:Number(data.get('weekly_goal')),color:data.get('color')});dialog.close();await refresh();toast(subject?'Subject updated.':'A new subject, a new possibility.');},subject?'Save subject':'Add subject');
}
function taskDialog(task=null, subjectId='') {
  if(!state.planner.subjects.length) {
    openDialog('Start with a subject.', 'A study session needs a subject to belong to.', `<p class="small muted">Add your first subject, then come back to plan a session.</p><div class="form-actions"><button class="button" data-action="add-subject">${icon('plus')}Add a subject</button></div>`);return;
  }
  openDialog(task?'Make the plan work for you.':'Make room for a little progress.',task?'Adjust your session without losing sight of the deadline.':'A focused session is a great place to start.',
    `<div class="form-stack">${field('What will you work on?','title','text','e.g. Practice polymorphism examples',{value:task?.title||'',required:true,maxlength:180})}<div class="field-grid">${selectField('Subject','subject_id',state.planner.subjects.map(subject=>[subject.id,`${subject.code} · ${subject.name}`]),task?.subject_id||subjectId||state.planner.subjects[0].id,'required')}${selectField('Session type','task_type',['STUDY','REVISION','ASSIGNMENT','EXAM'].map(type=>[type,typeLabel(type)]),task?.task_type||'STUDY')}${field('Planned date','scheduled_date','date','',{value:task?.scheduled_date||today(),required:true,min:'2000-01-01',max:'2100-12-31'})}${field('Start time','start_time','time','',{value:task?.start_time||'17:00',required:true})}${field('Duration (minutes)','duration','number','',{value:task?.duration||45,min:15,max:480,required:true})}${selectField('Priority','priority',[['LOW','Low · When there’s room'],['MEDIUM','Medium · Keep it moving'],['HIGH','High · Needs your attention']],task?.priority||'MEDIUM')}<label class="field span-2"><span>Actual deadline (optional)</span><input name="deadline" type="date" value="${h(task?.deadline||'')}" min="2000-01-01" max="2100-12-31"><small>Adaptive scheduling will never move this deadline.</small></label></div><label class="check-field"><input type="checkbox" name="adaptive" ${task?.adaptive!==false?'checked':''}> Allow adaptive rescheduling</label></div>`,
    async data=>{const values=Object.fromEntries(data);values.duration=Number(values.duration);values.adaptive=data.has('adaptive');if(task)values.version=task.version;await api(studentPath()+'/tasks'+(task?`/${task.id}`:''),task?'PUT':'POST',values);dialog.close();await refresh();toast(task?'Study session updated.':'A little progress is on the calendar.');},task?'Save session':'Plan session',task?`<button class="icon-button" type="button" data-action="delete-task" data-id="${task.id}" aria-label="Delete this session" title="Delete session">${icon('trash')}</button>`:'');
}
function noteDialog() {
  openDialog('A little guidance can go a long way.',`Leave a note for ${state.planner.student.name}.`,
    '<div class="form-stack"><label class="field"><span>Your note</span><textarea name="body" required maxlength="3000" placeholder="A little encouragement, a useful observation, or a next step…" rows="5"></textarea></label><label class="check-field"><input type="checkbox" name="shared" checked> Share this note with the student</label><div class="notice">Uncheck to keep this note visible only to administrators and the student’s current mentor.</div></div>',
    async data=>{await api(studentPath()+'/notes','POST',{body:data.get('body'),shared:data.has('shared')});dialog.close();await refresh();toast('Your note is saved.');},'Save note');
}

async function handleAction(button) {
  const action=button.dataset.action, id=button.dataset.id;
  if(action==='close-dialog'){dialog.close();credentialText='';return;}
  if(action==='menu'){const shellElement=document.querySelector('.shell');shellElement.classList.toggle('nav-open');button.setAttribute('aria-expanded',String(shellElement.classList.contains('nav-open')));return;}
  if(action==='navigate')return navigate(button.dataset.page);
  if(action==='refresh')return refresh();
  if(action==='reload')return initialize();
  if(action==='forgot'){openDialog('Let’s get you back in.', 'Your administrator can help you reset your password.', '<p>Ask your StudyFlow administrator to open your account and choose <strong>Reset password</strong>. They can share a new temporary password with you.</p><p class="small muted">For a student account, your mentor can help you contact the administrator.</p><div class="form-actions"><button class="button" data-action="close-dialog">Got it</button></div>');return;}
  if(action==='logout'){
    await api('/api/auth/logout','POST');++loadSequence;
    Object.assign(state,{me:null,csrf:'',users:[],planner:null,selected:null,setup:false});dialog.close();showAuth();return;
  }
  if(action==='add-student')return accountDialog('STUDENT');
  if(action==='add-mentor')return accountDialog('MENTOR');
  if(action==='add-admin')return accountDialog('ADMIN');
  if(action==='edit-user'){const user=state.users.find(user=>user.id===id)||state.planner?.student;if(user)return accountDialog(user.role,user);return;}
  if(action==='reset-password'){
    const user=state.users.find(user=>user.id===id);
    openDialog('Reset this account’s password?',`Existing sessions for ${user.name} will be signed out.`, '<p class="small muted">A new temporary password will be shown for you to share privately.</p>',async()=>{const result=await api(`/api/users/${id}/reset-password`,'POST');dialog.close();await refresh();credentialsDialog(result);},'Reset password');return;
  }
  if(action==='copy-credentials'){
    try{await navigator.clipboard.writeText(credentialText);dialog.querySelector('.copy-feedback').textContent='Copied. Share these details privately.';}catch{dialog.querySelector('.copy-feedback').textContent='Copy is unavailable here. Select and copy the details above manually.';}return;
  }
  if(action==='all-students')return navigate('students');
  if(action==='view-student')return navigate('students',id);
  if(action==='filter'){state.filter=button.dataset.filter;render();return;}
  if(action==='detail-tab'){state.detailTab=button.dataset.tab;state.query='';state.filter='all';render();return;}
  if(action.startsWith('go-')){
    const page=action.slice(3);
    if(state.selected){state.detailTab=page;state.query='';state.filter='all';render();return;}
    return navigate(page);
  }
  if(action==='add-subject')return subjectDialog();
  if(action==='edit-subject')return subjectDialog(state.planner.subjects.find(subject=>subject.id===id));
  if(action==='delete-subject')return confirmAction('Delete this subject?','Only a subject with no tasks can be deleted. This keeps your study history intact.',async()=>{await api(studentPath()+`/subjects/${id}`,'DELETE');toast('Subject deleted.');},'Delete subject');
  if(action==='add-task')return taskDialog(null,button.dataset.subject);
  if(action==='edit-task')return taskDialog(state.planner.tasks.find(task=>task.id===id));
  if(action==='delete-task')return confirmAction('Delete this study session?','This removes the session from your plan. Completed study time from this session will also be removed.',async()=>{await api(studentPath()+`/tasks/${id}`,'DELETE');toast('Study session deleted.');},'Delete session');
  if(action==='complete-task'){
    const task=state.planner.tasks.find(task=>task.id===id),completed=task.status!=='COMPLETED';
    await api(studentPath()+`/tasks/${id}/complete`,'POST',{completed,version:task.version});await refresh();toast(completed?'One more step forward. Nicely done.':'Session reopened.');return;
  }
  if(action==='reschedule-task'){
    const task=state.planner.tasks.find(task=>task.id===id);
    const result=await api(studentPath()+`/tasks/${id}/reschedule`,'POST',{version:task.version});await refresh();toast(`A fresh start: ${formatDate(result.scheduled_date)} at ${result.start_time}.`);return;
  }
  if(action==='add-note')return noteDialog();
  if(action==='delete-note')return confirmAction('Delete this note?','This note will be permanently removed.',async()=>{await api(studentPath()+`/notes/${id}`,'DELETE');toast('Note deleted.');},'Delete note');
  if(action==='week-prev'){state.week--;render();return;}
  if(action==='week-next'){state.week++;render();return;}
  if(action==='week-today'){state.week=0;render();return;}
}

document.addEventListener('click',async event=>{
  const button=event.target.closest('button[data-action]');
  if(!button||button.disabled)return;
  const initiallyDisabled=button.disabled;
  button.disabled=true;
  try{await handleAction(button);}catch(error){toast(error.message,true);}finally{button.disabled=initiallyDisabled;}
});
document.addEventListener('input',event=>{
  if(!event.target.matches('[data-search]'))return;
  const search=event.target.dataset.search,position=event.target.selectionStart;
  state.query=event.target.value;render();
  const input=root.querySelector(`[data-search="${search}"]`);input?.focus();input?.setSelectionRange(position,position);
});
document.addEventListener('keydown',event=>{
  if(event.key==='Escape')document.querySelector('.shell')?.classList.remove('nav-open');
});
document.addEventListener('click',event=>{
  const shellElement=document.querySelector('.shell.nav-open');
  if(shellElement&&!event.target.closest('.sidebar')&&!event.target.closest('[data-action="menu"]'))shellElement.classList.remove('nav-open');
});
document.addEventListener('submit',async event=>{
  const form=event.target;
  if(!form.matches('form[data-form]'))return;
  event.preventDefault();
  const button=form.querySelector('button[type="submit"]');
  if(button.disabled)return;
  const errorBox=form.querySelector('.form-error');errorBox.textContent='';
  button.disabled=true;const original=button.innerHTML;button.textContent='One moment…';
  try{
    const data=new FormData(form),values=Object.fromEntries(data),kind=form.dataset.form;
    if(data.has('confirm_password')&&values.password!==values.confirm_password)throw new Error('The passwords do not match. Please enter them again.');
    if(kind==='dialog'){await dialogSubmit(data,form);}
    else if(kind==='login'||kind==='setup'){
      const result=await api(kind==='setup'?'/api/setup':'/api/auth/login','POST',values);
      Object.assign(state,{me:result.user,csrf:result.csrf_token,today:result.today,setup:false,selected:null});
      if(state.me.must_change_password)showAuth();else await navigate();
    }else if(kind==='password'||kind==='first-password'){
      const result=await api('/api/me/password','POST',values);
      state.me=result.user;state.csrf=result.csrf_token;
      if(kind==='first-password')await navigate();else{form.reset();toast('Your password is updated. Other sessions have been signed out.');}
    }else if(kind==='profile'){
      state.me=await api('/api/me','PUT',{...values,daily_goal:Number(values.daily_goal)});
      await refresh();toast('Your preferences are saved.');
    }
  }catch(error){errorBox.textContent=error.message;errorBox.scrollIntoView({block:'nearest'});}
  finally{button.disabled=false;button.innerHTML=original;}
});
dialog.addEventListener('cancel',()=>{credentialText='';});

async function initialize(){
  try{
    const status=await api('/api/status');state.setup=status.setup_required;state.today=status.today;
    if(state.setup){showAuth();return;}
    try{const result=await api('/api/me');state.me=result.user;state.csrf=result.csrf_token;state.today=result.today;}
    catch(error){if(error.status!==401)throw error;showAuth();return;}
    if(state.me.must_change_password)showAuth();else await navigate();
  }catch(error){root.innerHTML=`<main id="main" class="offline">${brand()}<h1 style="margin-top:28px">Let’s reconnect.</h1><p class="muted">${h(error.message)}</p><button class="button" data-action="reload">${icon('refresh')}Try again</button></main>`;}
}
initialize();
