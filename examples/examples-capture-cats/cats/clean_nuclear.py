"""Clean the nuclear (biparental) tree of Li et al. 2016 Fig. 1A for PhyloSketch.
Input: 600-dpi render of the figure page; the tree region is cropped here."""
import sys, math, colorsys, numpy as np
from PIL import Image, ImageDraw
Image.MAX_IMAGE_PIXELS=None
exec(open('clean_tree.py').read().split('def main():')[0])   # erode, components_bbox, load

def dilate_sq(m,r):
    h=m.copy()
    for d in range(1,r+1): h|=np.roll(m,d,axis=1)|np.roll(m,-d,axis=1)
    v=h.copy()
    for d in range(1,r+1): v|=np.roll(h,d,axis=0)|np.roll(h,-d,axis=0)
    return v
def erode_sq(m,r):
    h=m.copy()
    for d in range(1,r+1): h&=np.roll(m,d,axis=1)&np.roll(m,-d,axis=1)
    v=h.copy()
    for d in range(1,r+1): v&=np.roll(h,d,axis=0)&np.roll(h,-d,axis=0)
    return v
def closing(m,r): return erode_sq(dilate_sq(m,r),r)

src,dst=sys.argv[1],sys.argv[2]
X0,X1,Y0,Y1=480,2490,2560,4760            # tree + label column in the 600-dpi page render
XL=2053-X0                                # label column start (labels are right of the leaf tips)
im=Image.open(src).convert('RGB').crop((X0,Y0,X1,Y1)); rgb=np.asarray(im).astype(np.int16); H,W,_=rgb.shape
r,g,b=[rgb[...,i]/255.0 for i in range(3)]
mx=np.maximum(np.maximum(r,g),b); mn=np.minimum(np.minimum(r,g),b); V=mx; Sat=np.where(mx>0,(mx-mn)/np.maximum(mx,1e-6),0)
lum=0.299*rgb[...,0]+0.587*rgb[...,1]+0.114*rgb[...,2]
# hue in degrees
hue=np.zeros_like(mx)
d=np.maximum(mx-mn,1e-6)
hue=np.where(mx==r,(60*((g-b)/d))%360,np.where(mx==g,60*((b-r)/d)+120,60*((r-g)/d)+240))
colour=(Sat>0.3)&(V>0.25)&~((hue>190)&(hue<245))      # coloured edges, no blue
black=lum<100
colour[:, XL:]=False                                   # nothing coloured counts in the label column
colour[:400,:660]=False; black[:400,:660]=False        # map inset
# --- asterisks: small red components ---
red=colour&(hue<15)|(colour&(hue>345))
red=erode(red,1)
for x0,y0,x1,y1,n in components_bbox(red):
    if x1-x0<=45 and y1-y0<=45: colour[max(y0-4,0):y1+5,max(x0-4,0):x1+5]=False
# --- numbered boxes (coloured or grey fills), detected before closing ---
fill=((Sat<=0.3)|((hue>48)&(hue<95)&(Sat<=0.95)))&(V>0.5)&(lum<245); fill[:, :650]=False; fill[:, 850:]=False; fill&=~black
fill=erode(fill,2)
cands=components_bbox(fill)
for bx in cands:
    if bx[4]>150: print("  candidate",bx[:4],"n=",bx[4],"mean rgb",rgb[bx[1]:bx[3]+1,bx[0]:bx[2]+1].reshape(-1,3).mean(axis=0).astype(int))
boxes=[bx for bx in cands if 40<=bx[2]-bx[0]<=140 and 20<=bx[3]-bx[1]<=90 and bx[4]>400]
print("boxes:",[(a,b_,c,d_) for a,b_,c,d_,_ in boxes])
pad=14; rects=[]
for x0,y0,x1,y1,_ in boxes:
    x0,y0,x1,y1=max(x0-pad,0),max(y0-pad,0),min(x1+pad,W-1),min(y1+pad,H-1); rects.append((x0,y0,x1,y1))
    colour[y0:y1+1,x0:x1+1]=False
# --- make dashed edges solid: link every dash end to the nearest other ink within 60 px ---
comps=components_bbox(colour)
dashes=[bx for bx in comps if bx[2]-bx[0]<=80 and bx[3]-bx[1]<=80 and bx[4]<2500]
print("dash components:",len(dashes),"of",len(comps))
img=Image.fromarray((colour*255).astype(np.uint8)).copy(); dr=ImageDraw.Draw(img)
R=60
for x0,y0,x1,y1,n in dashes:
    sub=colour[y0:y1+1,x0:x1+1]; ys,xs=np.nonzero(sub); pts=np.stack([xs+x0,ys+y0],axis=1)
    # two ends = extreme projections on the bbox diagonal (or the long axis)
    c=pts.mean(axis=0); cov=np.cov((pts-c).T.astype(float))
    w_,v_=np.linalg.eigh(cov); axis=v_[:,np.argmax(w_)]        # principal axis of the dash
    proj=(pts-c)@axis; ends=[(tuple(pts[np.argmin(proj)]),-axis),(tuple(pts[np.argmax(proj)]),axis)]
    own=np.zeros_like(colour); own[y0:y1+1,x0:x1+1]=sub
    for (ex,ey),(ux,uy) in ends:                                 # outward direction = principal axis
        wx0,wy0,wx1,wy1=max(ex-R,0),max(ey-R,0),min(ex+R,W-1),min(ey+R,H-1)
        cand=colour[wy0:wy1+1,wx0:wx1+1]&~own[wy0:wy1+1,wx0:wx1+1]
        cy,cx=np.nonzero(cand)
        if len(cy)==0: continue
        dx,dy=cx+wx0-ex,cy+wy0-ey; d2=dx**2+dy**2
        ahead=(dx*ux+dy*uy)>=0.7*np.sqrt(d2)                  # within a forward cone of 45 degrees
        if not ahead.any(): continue
        d2=np.where(ahead,d2,10**9); k=np.argmin(d2)
        if d2[k]<=R*R: dr.line([(int(ex),int(ey)),(int(cx[k]+wx0),int(cy[k]+wy0))],fill=255,width=12)
edges=np.array(img)>0
Image.fromarray(np.where(colour,0,255).astype(np.uint8)).save('dbg_colour.png')
Image.fromarray(np.where(edges,0,255).astype(np.uint8)).save('dbg_edges.png')
# --- bridge edges through the boxes ---
img=Image.fromarray((edges*255).astype(np.uint8)).copy(); dr=ImageDraw.Draw(img)
for x0,y0,x1,y1 in rects:
    ring=[]
    for band in range(2,26,4):
        ring+=[(x,y) for x in range(x0-band,x1+band+1) for y in (y0-band,y1+band) if 0<=x<W and 0<=y<H and edges[y,x]]
        ring+=[(x,y) for y in range(y0-band,y1+band+1) for x in (x0-band,x1+band) if 0<=x<W and 0<=y<H and edges[y,x]]
    clusters=[]
    for p in ring:
        for c in clusters:
            if any(abs(p[0]-q[0])<=30 and abs(p[1]-q[1])<=30 for q in c): c.append(p); break
        else: clusters.append([p])
    cents=[]
    for c in clusters:                       # snap the centroid to the nearest real ink point of the cluster
        mx,my=np.mean([p[0] for p in c]),np.mean([p[1] for p in c])
        cents.append(min(c,key=lambda q:(q[0]-mx)**2+(q[1]-my)**2))
    print("  box",(x0,y0,x1,y1),"entries:",cents)
    if len(cents)==2: dr.line([cents[0],cents[1]],fill=255,width=10)
    elif len(cents)==4:
        best=min(([(0,1),(2,3)],[(0,2),(1,3)],[(0,3),(1,2)]),key=lambda m: sum(math.dist(cents[i],cents[j]) for i,j in m))
        for i,j in best: dr.line([cents[i],cents[j]],fill=255,width=10)
    elif len(cents)>2:
        m=(int(np.mean([c[0] for c in cents])),int(np.mean([c[1] for c in cents])))
        for c in cents: dr.line([c,m],fill=255,width=10)
edges=np.array(img)>0
Image.fromarray(np.where(edges,0,255).astype(np.uint8)).save('dbg_edges2.png')
for pt in [(697,413),(760,393),(817,374),(830,372),(660,420)]:
    print("   edges at",pt,"=",bool(edges[pt[1],pt[0]]))
# --- keep the component connected to the root (leftmost edge pixel) ---
ys,xs=np.nonzero(edges); i=np.argmin(xs); seed=(int(xs[i]),int(ys[i])); print("root seed:",seed)
from collections import deque
def reach8(mask,seed):
    seen=np.zeros_like(mask); q=deque([seed]); seen[seed[1],seed[0]]=True
    while q:
        x,y=q.popleft()
        for dx,dy in ((1,0),(-1,0),(0,1),(0,-1),(1,1),(1,-1),(-1,1),(-1,-1)):
            nx,ny=x+dx,y+dy
            if 0<=nx<W and 0<=ny<H and mask[ny,nx] and not seen[ny,nx]:
                seen[ny,nx]=True; q.append((nx,ny))
    return seen
tree=reach8(edges,seed); print("edge pixels dropped:",int((edges&~tree).sum()))
for pt in [(697,413),(760,393),(817,374),(830,372),(660,420),(54,1809)]:
    print("   tree at",pt,"=",bool(tree[pt[1],pt[0]]))
# --- labels: black ink in the label column; remove symbols right of each label (gap >= 40 px) ---
text=black.copy(); text[:, :XL]=False; text[2170:, :]=False   # drop the axis label at the bottom
stub=tree[:, XL-80:XL-5].any(axis=1)                   # leaf tips just left of the label column
rows=np.nonzero(stub)[0]; leaves=[]
for rr in rows:
    if leaves and rr-leaves[-1][1]<=3: leaves[-1][1]=rr
    else: leaves.append([rr,rr])
print("leaf rows:",len(leaves))
for a,b_ in leaves:
    yc=(a+b_)//2; y0,y1=max(yc-30,0),min(yc+30,H)
    colb=text[y0:y1].any(axis=0); started=False; run=0; cut=None
    for x in range(XL,W):
        if colb[x]: started=True; run=0
        elif started:
            run+=1
            if run>=30: cut=x-run+1; break
    print("   row y=%d label ink from %d, cut at %s"%(yc, int(np.argmax(colb)) if colb.any() else -1, cut))
    if cut is not None and colb[cut:].any():
        text[y0:y1,cut:]=False
out=np.where(tree|text,0,255).astype(np.uint8)
Image.fromarray(out).save(dst); print("saved",dst,out.shape)
