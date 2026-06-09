"""
SSL Connector KIT & Mirroring - Business Logic Flow Diagram
Genera un PNG ad alta risoluzione del flusso logico di business.
"""

import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
from matplotlib.patches import FancyBboxPatch
import textwrap

# ─────────────────────────────────────────────
# PALETTE
# ─────────────────────────────────────────────
BG        = '#0f1117'
LANE_BG   = '#161b26'

COLS = {
    'jms':    {'hdr': '#1e3a5f', 'hdr_txt': '#5ea8f5', 'blk': '#1a2b42', 'border': '#2d5a8e',  'acc': '#2d6bc4'},
    'sess':   {'hdr': '#2d1f4e', 'hdr_txt': '#b794f4', 'blk': '#221840', 'border': '#4a2f9a',  'acc': '#6b46c1'},
    'eng':    {'hdr': '#0d3d3d', 'hdr_txt': '#4ecdc4', 'blk': '#0a2e2e', 'border': '#1a6b60',  'acc': '#2a9d8f'},
    'cls':    {'hdr': '#3d2200', 'hdr_txt': '#f6ad55', 'blk': '#2d1a00', 'border': '#9c4b00',  'acc': '#dd6b20'},
    'mbom':   {'hdr': '#0a3d1a', 'hdr_txt': '#68d391', 'blk': '#072b12', 'border': '#1a5c32',  'acc': '#276749'},
    'kit':    {'hdr': '#3d0a2a', 'hdr_txt': '#f687b3', 'blk': '#2d0520', 'border': '#8b2563',  'acc': '#b83280'},
    'ca':     {'hdr': '#3d3300', 'hdr_txt': '#f6e05e', 'blk': '#2d2500', 'border': '#8a6700',  'acc': '#b7791f'},
    'lc':     {'hdr': '#3d0f0f', 'hdr_txt': '#fc8181', 'blk': '#2d0808', 'border': '#8b1a1a',  'acc': '#c53030'},
}

VERB_GET   = '#63b3ed'
VERB_POST  = '#68d391'
VERB_PATCH = '#ecc94b'
COND_BG    = '#1e2433'
COND_BORDER= '#3d5a80'

# ─────────────────────────────────────────────
# FIGURE SETUP
# ─────────────────────────────────────────────
FIG_W, FIG_H = 26, 16
DPI = 160
fig, ax = plt.subplots(figsize=(FIG_W, FIG_H), dpi=DPI)
fig.patch.set_facecolor(BG)
ax.set_facecolor(BG)
ax.set_xlim(0, FIG_W)
ax.set_ylim(0, FIG_H)
ax.axis('off')

LANES    = 8
LW       = FIG_W / LANES          # lane width
TOP      = FIG_H - 0.35
HDR_H    = 0.55
PAD      = 0.13
GAP      = 0.10

# ─────────────────────────────────────────────
# HELPER FUNCTIONS
# ─────────────────────────────────────────────

def lane_x(i):
    """Left x of lane i (0-based)."""
    return i * LW + 0.08

def draw_rect(x, y, w, h, fc, ec, lw=0.8, radius=0.12, zorder=2):
    rect = FancyBboxPatch((x, y), w, h,
                          boxstyle=f"round,pad=0,rounding_size={radius}",
                          facecolor=fc, edgecolor=ec, linewidth=lw, zorder=zorder)
    ax.add_patch(rect)

def txt(x, y, s, size=6.5, color='#e0e0e0', ha='left', va='top',
        weight='normal', wrap_w=None, zorder=3):
    if wrap_w:
        lines = []
        for line in s.split('\n'):
            lines += textwrap.wrap(line, wrap_w) if line.strip() else ['']
        s = '\n'.join(lines)
    ax.text(x, y, s, fontsize=size, color=color, ha=ha, va=va,
            fontweight=weight, zorder=zorder,
            fontfamily='DejaVu Sans')

def verb_badge(x, y, verb):
    colors = {'GET': ('#1e4d6b', VERB_GET),
              'POST': ('#1e4d1e', VERB_POST),
              'PATCH': ('#4d3b00', VERB_PATCH)}
    bg, fg = colors.get(verb, ('#333', '#fff'))
    draw_rect(x, y-0.13, 0.35, 0.16, bg, fg, lw=0.6, radius=0.05)
    txt(x+0.175, y-0.05, verb, size=5, color=fg, ha='center', va='center', weight='bold')

def draw_arrow_down(x, y, col_acc):
    ax.annotate('', xy=(x, y - 0.12), xytext=(x, y),
                arrowprops=dict(arrowstyle='->', color=col_acc, lw=1.2),
                zorder=4)

def lane_header(i, title, col_key):
    c = COLS[col_key]
    x = lane_x(i)
    w = LW - 0.16
    draw_rect(x, TOP - HDR_H, w, HDR_H, c['hdr'], c['acc'], lw=1.2, radius=0.15)
    ax.text(x + w/2, TOP - HDR_H/2, title,
            fontsize=6.8, color=c['hdr_txt'], ha='center', va='center',
            fontweight='bold', zorder=3,
            fontfamily='DejaVu Sans')

def block(i, y_top, col_key, title, lines, verb=None, height=None):
    """Draw a block in lane i at y_top. Returns bottom y."""
    c    = COLS[col_key]
    x    = lane_x(i)
    w    = LW - 0.16
    # estimate height
    n_lines = sum(1 + line.count('\n') for line in lines) + 2
    h = height if height else max(0.55, 0.22 + n_lines * 0.155)
    draw_rect(x, y_top - h, w, h, c['blk'], c['border'], lw=0.8)
    ty = y_top - 0.10
    if verb:
        verb_badge(x + 0.05, ty, verb)
        tx = x + 0.44
    else:
        tx = x + 0.08
    txt(tx, ty, title, size=6.3, color=c['hdr_txt'], weight='bold', wrap_w=22)
    ty -= 0.22
    for line in lines:
        if line.startswith('§'):          # sub-header
            txt(x+0.08, ty, line[1:], size=5.4, color='#7a8fa6', weight='bold')
            ty -= 0.17
        elif line.startswith('•'):        # attribute
            txt(x+0.08, ty, '>> ' + line[1:], size=5.7, color='#c9d1d9', wrap_w=26)
            ty -= 0.16
        elif line.startswith('!'):        # warning
            txt(x+0.08, ty, '[!] ' + line[1:], size=5.5, color='#fc8181', wrap_w=26)
            ty -= 0.16
        elif line.startswith('>'):        # note
            txt(x+0.08, ty, line[1:], size=5.2, color='#556677', wrap_w=26)
            ty -= 0.14
        else:
            txt(x+0.08, ty, line, size=5.7, color='#c9d1d9', wrap_w=26)
            ty -= 0.16
    return y_top - h

def cond_block(i, y_top, question, branches, height=None):
    """Draw a diamond-style condition block."""
    x  = lane_x(i)
    w  = LW - 0.16
    h  = height if height else max(0.55, 0.24 + len(branches)*0.20)
    draw_rect(x, y_top-h, w, h, COND_BG, COND_BORDER, lw=0.8, radius=0.10)
    # dashed border re-draw
    rect2 = FancyBboxPatch((x, y_top-h), w, h,
                           boxstyle="round,pad=0,rounding_size=0.10",
                           facecolor='none', edgecolor=COND_BORDER,
                           linewidth=0.6, linestyle='--', zorder=3)
    ax.add_patch(rect2)
    txt(x+0.08, y_top-0.10, '[?] ' + question, size=5.8, color='#e8b84b', weight='bold')
    ty = y_top - 0.27
    branch_colors = ['#63b3ed','#68d391','#f6ad55','#fc8181','#b794f4','#4ecdc4']
    for idx, (sym, label) in enumerate(branches):
        bc = branch_colors[idx % len(branch_colors)]
        ax.plot([x+0.10, x+0.14], [ty+0.06, ty+0.06], color=bc, lw=1.2, zorder=4)
        txt(x+0.17, ty+0.09, sym + ' ' + label, size=5.5, color=bc, wrap_w=22)
        ty -= 0.19
    return y_top - h

def arrow(i, y):
    c = list(COLS.values())[i]
    xc = lane_x(i) + (LW-0.16)/2
    ax.annotate('', xy=(xc, y - 0.10), xytext=(xc, y),
                arrowprops=dict(arrowstyle='->', color=c['acc'], lw=1.1), zorder=4)

def cross_arrow(i_from, i_to, y, color='#3a4d6a', label=''):
    """Horizontal arrow between lanes at height y."""
    x_from = lane_x(i_from) + (LW-0.16)
    x_to   = lane_x(i_to)
    xm = (x_from + x_to) / 2
    ax.annotate('', xy=(x_to, y), xytext=(x_from, y),
                arrowprops=dict(arrowstyle='->', color=color, lw=0.9,
                                connectionstyle='arc3,rad=0'), zorder=4)
    if label:
        txt(xm, y + 0.07, label, size=4.8, color=color, ha='center')

# ─────────────────────────────────────────────
# TITLE & LEGEND
# ─────────────────────────────────────────────
ax.text(FIG_W/2, FIG_H - 0.15,
        'SSL CONNECTOR KIT & MIRRORING — BUSINESS LOGIC FLOW',
        fontsize=11, color='white', ha='center', va='top',
        fontweight='bold', fontfamily='DejaVu Sans', zorder=5)

legend_items = [
    (VERB_GET,   '#1e4d6b', 'GET (lettura)'),
    (VERB_POST,  '#1e4d1e', 'POST (creazione)'),
    (VERB_PATCH, '#4d3b00', 'PATCH (modifica)'),
    ('#e8b84b',  COND_BG,   'Condizione / Check'),
    ('#9ae6b4',  '#071a07', '[OK] Acknowledge JMS'),
]
lx = 1.0
for fc, bg, lbl in legend_items:
    draw_rect(lx, FIG_H-0.34, 0.22, 0.14, bg, fc, lw=0.7, radius=0.04)
    txt(lx + 0.27, FIG_H - 0.25, lbl, size=5.5, color='#7a8fa6')
    lx += 2.5

# ─────────────────────────────────────────────
# LANE HEADERS
# ─────────────────────────────────────────────
headers = [
    (0, '1. JMS Input Event',         'jms'),
    (1, '2. Sessione / HTTP Client',   'sess'),
    (2, '3. Engineering Item',         'eng'),
    (3, '4. Classificazione MFN',      'cls'),
    (4, '5. Mirror MBOM',              'mbom'),
    (5, '6. Gestione KIT',             'kit'),
    (6, '7. Change Action',            'ca'),
    (7, '8. Lifecycle & Approvazione', 'lc'),
]
for i, title, ck in headers:
    lane_header(i, title, ck)

# ─────────────────────────────────────────────
# LANE 0 — JMS INPUT EVENT
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = block(0, y, 'jms', 'TextMessage ricevuto', [
    '§Broker SSL / ActiveMQ',
    '•Topic: 3dsevents.OI000000677\n  .3DSpace.user',
    '•Consumer: BatchUser-Mirror\n  MBOM-KIT_QUALITY',
    '•Protocol: SSL port 61617',
    '•Failover: 2 broker',
    '•Redelivery: INFINITA',
], height=1.45)
arrow(0, y); y -= 0.10 + GAP

y = block(0, y, 'jms', 'Parsing JSON payload', [
    '§Campi estratti',
    '•eventType',
    '•eventClass',
    '•subjectId  (MFN Physical ID)',
    '•subjectType',
    '•nextState',
    '•authorization → company,\n  collaborativeSpace, user',
])
arrow(0, y); y -= 0.10 + GAP

y = cond_block(0, y, 'FILTRO EVENTO', [
    ('[OK]', 'eventType="created" +\n   eventClass="Manufacturing Item"\n   → ELABORA'),
    ('[NO]', 'altri eventi → IGNORA'),
], height=0.80)
arrow(0, y); y -= 0.10 + GAP

y = block(0, y, 'jms', 'Build Security Context', [
    '•Formato: Role.Company\n  .CollaborativeSpace',
    '•Role fisso: VPLMProjectLeader',
    '•Header: SecurityContext',
])

# ACK block at bottom
y_ack = 0.65
draw_rect(lane_x(0), y_ack, LW-0.16, 0.50, '#071a07', '#276729', lw=1.0, radius=0.10)
txt(lane_x(0)+0.08, y_ack+0.40, '[OK] message.acknowledge()', size=6.2, color='#9ae6b4', weight='bold')
txt(lane_x(0)+0.08, y_ack+0.24, '  Solo dopo elaborazione completa', size=5.4, color='#556677')
txt(lane_x(0)+0.08, y_ack+0.12, '  CLIENT_ACKNOWLEDGE', size=5.2, color='#276749')

# ─────────────────────────────────────────────
# LANE 1 — SESSION / HTTP CLIENT
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = block(1, y, 'sess', 'SessionManager.getClient()', [
    '§Cache check',
    '•Key: userKey',
    '•TTL: 20 minuti',
    '•Thread-safe: double-check lock',
    '•Map: ConcurrentHashMap',
])
arrow(1, y); y -= 0.10 + GAP

y = cond_block(1, y, 'CACHE HIT?', [
    ('[OK]', 'Sessione valida → riusa\n   Client3DXImpl esistente'),
    ('[NO]', 'Scaduta/assente → crea\n   nuovo client + init()'),
], height=0.80)
arrow(1, y); y -= 0.10 + GAP

y = block(1, y, 'sess', 'Client3DXImpl.init()', [
    '§Inizializzazione HTTPS',
    '•Legge: Client3dExperience\n  .properties',
    '•SSL: TLSContext default JDK',
    '•Timeout: 2 minuti',
    '•CookieHandler: ON',
])
arrow(1, y); y -= 0.10 + GAP

y = block(1, y, 'sess', 'Properties caricate', [
    '•url_3dSpace (base URL enovia)',
    '•url_clm (OAuth2 endpoint)',
    '•url_createMBOM',
    '•clm_agent_id',
    '•clm_agent_pwd (URL-encoded)',
    '•securityContext',
])
arrow(1, y); y -= 0.10 + GAP

y = block(1, y, 'sess', 'CSRF Token (prima GET)', [
    '•Header risposta: ENO_CSRF_TOKEN',
    '•Obbligatorio su ogni POST',
])
arrow(1, y); y -= 0.10 + GAP

y = block(1, y, 'sess', 'Retry Policy HTTP', [
    '•Tentativi: 3',
    '•Backoff: 1s → 2s → 4s',
    '•Trigger: IOException',
    '•Non-200: Exception immediata',
])

# ─────────────────────────────────────────────
# LANE 2 — ENGINEERING ITEM
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = block(2, y, 'eng', 'getScopedEngItem(mfnId)', [
    '§Input',
    '•subjectId  (MFN Physical ID)',
    '§Output',
    '•engId (Engineering Physical ID)',
], verb='GET')
arrow(2, y); y -= 0.10 + GAP

y = cond_block(2, y, 'engId trovato?', [
    ('[OK]', 'engId valido → prosegui'),
    ('[!]',  'null/blank → scheduleRetry()'),
], height=0.68)

# retry annotation
rx = lane_x(2) + 0.08
ry = y - 0.06
draw_rect(rx, ry-0.38, LW-0.32, 0.38, '#1a1a00', '#8a7a00', lw=0.7, radius=0.08)
txt(rx+0.06, ry-0.04, '[T] Retry schedulato: +15 min', size=5.4, color='#d4c44b', weight='bold')
txt(rx+0.06, ry-0.18, 'ScheduledExecutorService', size=5.2, color='#9a8a30')
txt(rx+0.06, ry-0.30, '→ richiama mfnRootManagement()', size=5.2, color='#9a8a30')
y = ry - 0.38

arrow(2, y); y -= 0.10 + GAP

y = block(2, y, 'eng', 'getEngineeringItemById(engId)', [
    '§Attributi letti',
    '•title / name',
    '•revision  (seq. COMAU A→Z)',
    '•partNumber / COMAU_Code',
    '•COMAU_Eng_IsMBOMrequired',
    '•COMAU_Eng_IsKITrequired',
    '•state / policy / cestamp',
], verb='GET')
arrow(2, y); y -= 0.10 + GAP

y = block(2, y, 'eng', 'Check attributi COMAU', [
    '•IsMBOMrequired=YES → MBOM flow',
    '•IsKITrequired=YES  → KIT flow',
    '•entrambi NO → solo classificazione',
])

# ─────────────────────────────────────────────
# LANE 3 — CLASSIFICAZIONE
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = block(3, y, 'cls', 'getWhereClassified(engId)', [
    '§Input',
    '•engId',
    '§Output',
    '•Lista classId[] (Library 3DX)',
    '•Classi padre dell\'EngItem',
], verb='GET')
arrow(3, y); y -= 0.10 + GAP

y = block(3, y, 'cls', 'Loop per ogni classId', [
    '•Errori individuali loggati',
    '•Non bloccanti (continue)',
    '•Un errore non ferma le altre\n  classificazioni',
], height=0.72)
arrow(3, y); y -= 0.10 + GAP

y = block(3, y, 'cls', 'classifyObject()', [
    '§Input per classe',
    '•classID',
    '•type  (MFN type string)',
    '•identifier  (MFN Physical ID)',
    '•relativePath:',
    '  /dslc/changeaction/  oppure',
    '  /dsmfg/dsmfg:MfgItem/',
    '§Oggetto creato',
    '•ClassifiedItem link: MFN↔Class',
], verb='POST')
arrow(3, y); y -= 0.10 + GAP

y = block(3, y, 'cls', 'Risultato classificazione', [
    '•MFN visibile in Library 3DX',
    '•Classificato in tutte le classi\n  dell\'Engineering Item associato',
    '>→ visibilità Library garantita',
])

# ─────────────────────────────────────────────
# LANE 4 — MBOM
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = cond_block(4, y, 'CA_MBOMMirrorBehavior', [
    ('[OK]', 'CREATE_MBOM'),
    ('[OK]', 'CREATE_AND_RELEASE_MBOM'),
    ('[ ]', 'DO_NOTHING → skip lane'),
], height=0.72)
arrow(4, y); y -= 0.10 + GAP

y = block(4, y, 'mbom', 'expandEngineeringItem(engId)', [
    '§EBOM attuale (depth: ALL)',
    '•Output: struttura figli',
    '•Mappa: physicalId → qty',
], verb='POST')
arrow(4, y); y -= 0.10 + GAP

y = block(4, y, 'mbom', 'getRevisionsPath()', [
    '§Input: subjectId MFN',
    '•Output: revisione MFN precedente',
    '•Seq. COMAU: A→B→C→D…→Z',
], verb='POST')
arrow(4, y); y -= 0.10 + GAP

y = block(4, y, 'mbom', 'expandEngineeringItem(prev)', [
    '§EBOM revisione precedente',
    '•Output: mappa qty precedente',
], verb='POST')
arrow(4, y); y -= 0.10 + GAP

y = block(4, y, 'mbom', 'findDifferences()', [
    '•Componenti aggiunti\n  → crea nuove istanze MFN',
    '•Componenti rimossi\n  → PATCH qty con commento',
    '•Return 0=OK, 1=errore',
])
arrow(4, y); y -= 0.10 + GAP

y = block(4, y, 'mbom', 'createMFGItemStructure()', [
    '§Oggetto MBOM creato',
    '•EngItemId',
    '•ManItemId',
    '•XML link authoring config',
    '•CSRF token obbligatorio',
], verb='POST')
arrow(4, y); y -= 0.10 + GAP

y = block(4, y, 'mbom', 'createNewManufacturingItem\nInstanceUnderChange()', [
    '•parentMFNId',
    '•changeId',
    '•body: mfnId figlio',
], verb='POST', height=0.72)

# ─────────────────────────────────────────────
# LANE 5 — KIT
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = cond_block(5, y, 'COMAU_CABS_KITManagement', [
    ('[OK]', 'CREATE_KIT'),
    ('[OK]', 'CREATE_AND_RELEASE_KIT'),
    ('[ ]', 'DO_NOTHING → skip lane'),
], height=0.72)
arrow(5, y); y -= 0.10 + GAP

y = block(5, y, 'kit', 'createNewManufacturingItem() KIT', [
    '§Attributi valorizzati',
    '•type: Provide / CreateKit',
    '•COMAU_MFG_ItemLevel',
    '•COMAU_MFG_ItemType',
    '•Title (dall\'EngItem title)',
    '•securityContext',
    '§Output',
    '•kitId (nuovo MFN KIT)',
], verb='POST')
arrow(5, y); y -= 0.10 + GAP

y = block(5, y, 'kit', 'expandManufacturingItem(kitId)', [
    '§Struttura MBOM KIT (1° livello)',
    '•Output: figli non-released',
], verb='POST')
arrow(5, y); y -= 0.10 + GAP

y = block(5, y, 'kit', 'patchModifyManufacturingUnder\nChange()', [
    '§Attributi modificati',
    '•cestamp  (optimistic lock)',
    '•removedQuantity (commento)',
    '•changeId',
], verb='PATCH', height=0.80)
arrow(5, y); y -= 0.10 + GAP

y = block(5, y, 'kit', 'fillProposedChange()', [
    '•Lista figli non-released',
    '•PROPOSEDCHANGES[] array',
    '•type per ogni item',
])

# ─────────────────────────────────────────────
# LANE 6 — CHANGE ACTION
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = block(6, y, 'ca', 'createChangeAction()', [
    '§Oggetto creato',
    '•title (da EngItem title)',
    '•purpose / description',
    '•type: CreateAssembly/Provide',
    '•securityContext',
    '§Output',
    '•changeId',
], verb='POST')
arrow(6, y); y -= 0.10 + GAP

y = block(6, y, 'ca', 'patchChange() — attributi custom', [
    '§Attributi valorizzati',
    '•cestamp  (optimistic lock)',
    '•COMAU_CA_MBOMMirrorBehavior',
    '•COMAU_CABS_KITManagement',
    '•COMAU_CABS_RelatedEngId',
    '•assignees[]  (utenti)',
    '•approvers[]  (utenti)',
    '•PROPOSEDCHANGES[]',
    '•mfnType / compareIds / kitAction',
], verb='PATCH')
arrow(6, y); y -= 0.10 + GAP

y = block(6, y, 'ca', 'classifyObject() su Change', [
    '•classId[] (da getWhereClassified)',
    '•relativePath: /dslc/changeaction/',
    '•Stesse classi dell\'EngItem',
], verb='POST')
arrow(6, y); y -= 0.10 + GAP

y = block(6, y, 'ca', 'changeOwner()', [
    '§Trasferimento ownership',
    '•company',
    '•collaborativeSpace',
    '•user  (dall\'evento JMS)',
    '•changeId',
], verb='POST')

# ─────────────────────────────────────────────
# LANE 7 — LIFECYCLE
# ─────────────────────────────────────────────
y = TOP - HDR_H - GAP

y = block(7, y, 'lc', 'promoteObject()', [
    '§Cambio stato Change Action',
    '•id: changeId',
    '•nextState: "In Approval"',
    '•endpoint: /dslc/maturity/\n  changeState',
], verb='POST')
arrow(7, y); y -= 0.10 + GAP

y = block(7, y, 'lc', 'getChangeActionById(changeId)', [
    '§Dati per approvazione',
    '•cestamp aggiornato',
    '•approvers list',
    '•current state',
], verb='GET')
arrow(7, y); y -= 0.10 + GAP

y = block(7, y, 'lc', 'approveChangeActionById()', [
    '§Approvazione automatica',
    '•changeActionId',
    '•cestamp  (optimistic lock)',
    '•approvers valorizzati',
    '•Stato finale: APPROVED',
], verb='POST')
arrow(7, y); y -= 0.10 + GAP

y = cond_block(7, y, 'Se CREATE_AND_RELEASE?', [
    ('[OK]', 'promoteObject() MFN\n   → stato RELEASED'),
    ('[ ]', 'Solo CREATE → resta in Work'),
], height=0.70)
arrow(7, y); y -= 0.10 + GAP

y = block(7, y, 'lc', 'createIssue()  [se errore]', [
    '§Tracciamento anomalie',
    '•title  (descrizione errore)',
    '•assignee  (utente evento)',
    '•reviewers',
    '•resolvedBy: changeId',
    '>→ stato: "In Review" automatico',
], verb='POST')

# ─────────────────────────────────────────────
# CROSS-LANE ARROWS (flow connections)
# ─────────────────────────────────────────────
y_cross = TOP - HDR_H - 0.30
cross_arrow(0, 1, y_cross, '#3a4d6a', 'subjectId + auth')
cross_arrow(1, 2, y_cross - 0.10, '#3a4d6a', 'Client3DXImpl')
cross_arrow(2, 3, y_cross - 0.20, '#3a4d6a', 'engId')
cross_arrow(3, 4, y_cross - 0.30, '#3a4d6a', 'MBOM flag')
cross_arrow(3, 5, y_cross - 0.42, '#3a4d6a', 'KIT flag')
cross_arrow(4, 6, y_cross - 0.54, '#3a4d6a', 'diffs')
cross_arrow(5, 6, y_cross - 0.66, '#3a4d6a', 'kitId')
cross_arrow(6, 7, y_cross - 0.78, '#3a4d6a', 'changeId')

# ─────────────────────────────────────────────
# FOOTER
# ─────────────────────────────────────────────
ax.text(FIG_W/2, 0.10,
        'com.comau:sslConnector v0.0.1-SNAPSHOT  |  Java 11  |  ActiveMQ SSL  |  '
        '3DEXPERIENCE OI000000677-eu1  |  JMS CLIENT_ACKNOWLEDGE',
        fontsize=5.5, color='#3a4d5c', ha='center', va='bottom',
        fontfamily='DejaVu Sans', zorder=5)

# ─────────────────────────────────────────────
# SAVE
# ─────────────────────────────────────────────
out = r'D:\OneDrive - S3K S.p.A\EclipseWS\sslConnectorKITandMirroring\flow_diagram.png'
plt.tight_layout(pad=0.2)
plt.savefig(out, dpi=DPI, bbox_inches='tight',
            facecolor=BG, edgecolor='none')
plt.close()
print(f'Saved: {out}')
