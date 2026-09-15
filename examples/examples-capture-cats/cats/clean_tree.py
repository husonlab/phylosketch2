"""Clean a published tree figure for capture with PhyloSketch.
Usage: clean_tree.py in.png out.png [--flip]
Steps: binarize; white out given regions; detect coloured boxes on edges and
bridge the edge through them; keep only the component connected to the root
in the tree region; optionally mirror the tree so that the root is on the left."""
import sys, numpy as np
from PIL import Image, ImageDraw

def load(path):
    im=Image.open(path).convert('RGB'); return np.asarray(im).astype(np.int16)

def erode(m,r):
    out=m.copy()
    for dy in range(-r,r+1):
        for dx in range(-r,r+1):
            out&=np.roll(np.roll(m,dy,axis=0),dx,axis=1)
    return out

def components_bbox(mask):
    """bounding boxes (x0,y0,x1,y1,size) of connected components of a boolean mask"""
    img=Image.fromarray(np.where(mask,255,0).astype(np.uint8)).copy(); boxes=[]
    while True:
        arr=np.array(img); ys,xs=np.nonzero(arr==255)
        if len(ys)==0: break
        ImageDraw.floodfill(img,(int(xs[0]),int(ys[0])),128)
        arr=np.array(img); comp=(arr==128); cy,cx=np.nonzero(comp)
        boxes.append((int(cx.min()),int(cy.min()),int(cx.max()),int(cy.max()),int(comp.sum())))
        arr[comp]=64; img=Image.fromarray(arr).copy()
    return boxes

def main():
    src,dst=sys.argv[1],sys.argv[2]; flip='--flip' in sys.argv
    rgb=load(src); H,W,_=rgb.shape
    lum=(0.299*rgb[...,0]+0.587*rgb[...,1]+0.114*rgb[...,2])
    sat=rgb.max(axis=2)-rgb.min(axis=2)
    black=lum<110                                    # ink: edges and text
    # ---- regions to white out (mitogenome image, 1714x2196) ----
    white=np.zeros((H,W),bool)
    white[:70,650:1010]=True                          # title
    white[:640,1080:]=True                            # legend
    black&=~white
    # label column boundary: first empty column left of x=400 across all rows
    colink=black[:, :400].any(axis=0)
    XB=400
    for x in range(399,300,-1):
        if not colink[x]: XB=x; break
    print("label/tree boundary XB =",XB)
    # leaf rows: horizontal ink runs in the stub zone just right of XB
    stub=black[:, XB+2:XB+30].all(axis=1)
    rows=np.nonzero(stub)[0]; leaves=[]
    for r in rows:
        if leaves and r-leaves[-1][1]<=2: leaves[-1][1]=r
        else: leaves.append([r,r])
    leafy=[(a+b)//2 for a,b in leaves]; print("leaf rows:",len(leafy))
    # left-side symbols: in each leaf band, white out everything left of the last gap (>=10 empty columns) before x=130
    for yc in leafy:
        y0,y1=max(yc-24,0),min(yc+24,H)
        colb=black[y0:y1,:130].any(axis=0)
        cut=0; run=0
        for x in range(130):
            run=run+1 if not colb[x] else 0
            if run>=10: cut=x+1
        if cut>0 and colb[:cut].any():
            black[y0:y1,:cut]=False
    # second pass: thin leftover dash fragments in the far-left strip (not the dot of an 'i')
    strip=black.copy(); strip[:, 100:]=False
    for x0,y0,x1,y1,n in components_bbox(strip):
        if y1-y0<=12 and x1-x0<=40:
            below=black[y1+2:y1+8, x0:x1+1].any() if y1+8<H else False
            if not below: black[y0:y1+1,x0:x1+1]=False
    # ---- coloured boxes on the tree (x>400, outside legend) ----
    fill=(sat>20)|((lum>150)&(lum<240)&(sat<=20))     # coloured or grey fill
    fill[:, :400]=False; fill[:640,1080:]=False; fill&=~black
    fill=erode(fill,3)
    boxes=[b for b in components_bbox(fill) if 40<=b[2]-b[0]<=130 and 20<=b[3]-b[1]<=80 and b[4]>400]
    print("boxes:",[(int(a),int(b),int(c),int(d)) for a,b,c,d,_ in boxes])
    pad=10
    for x0,y0,x1,y1,_ in boxes:
        x0,y0,x1,y1=max(x0-pad,0),max(y0-pad,0),min(x1+pad,W-1),min(y1+pad,H-1)
        # entry points: black pixels on a ring 2px outside the rectangle
        ring=[]
        for x in range(x0-2,x1+3):
            for y in (y0-2,y1+2):
                if 0<=x<W and 0<=y<H and black[y,x]: ring.append((x,y))
        for y in range(y0-2,y1+3):
            for x in (x0-2,x1+2):
                if 0<=x<W and 0<=y<H and black[y,x]: ring.append((x,y))
        black[y0:y1+1,x0:x1+1]=False
        # cluster ring points
        clusters=[]
        for p in ring:
            for c in clusters:
                if any(abs(p[0]-q[0])<=6 and abs(p[1]-q[1])<=6 for q in c): c.append(p); break
            else: clusters.append([p])
        cents=[(int(np.mean([p[0] for p in c])),int(np.mean([p[1] for p in c]))) for c in clusters]
        print("  box",(x0,y0,x1,y1),"entries:",cents)
        img=Image.fromarray((black*255).astype(np.uint8)); d=ImageDraw.Draw(img)
        if len(cents)==2: d.line([cents[0],cents[1]],fill=255,width=4)
        elif len(cents)==4:
            import itertools, math
            best=min(([(0,1),(2,3)],[(0,2),(1,3)],[(0,3),(1,2)]),key=lambda m: sum(math.dist(cents[i],cents[j]) for i,j in m))
            for i,j in best: d.line([cents[i],cents[j]],fill=255,width=4)
        elif len(cents)>2:
            m=(int(np.mean([c[0] for c in cents])),int(np.mean([c[1] for c in cents])))
            for c in cents: d.line([c,m],fill=255,width=4)
        black=np.array(img)>0
    # ---- keep only the tree component in the tree region ----
    img=Image.fromarray((black*255).astype(np.uint8)).copy()
    ys,xs=np.nonzero(black[:, :]); order=np.argsort(-xs)
    seed=None
    for i in order[:2000]:
        x,y=int(xs[i]),int(ys[i])
        if x<1080 or y>640: seed=(x,y); break       # rightmost ink outside the legend area = root stub
    print("seed (root):",seed)
    ImageDraw.floodfill(img,seed,128)
    a=np.array(img); tree=(a==128)
    keep=tree.copy(); keep[:, :XB]|=black[:, :XB]   # keep all ink in the label column
    removed=int((black&~keep).sum()); print("ink pixels removed in tree region:",removed)
    out=np.where(keep,0,255).astype(np.uint8)
    if flip:
        treepart=out[:,XB:][:, ::-1]; labels=out[:, :XB].copy()
        # left-align each label row so that it starts 30 px after the leaf tip
        rows=np.nonzero((labels<128).any(axis=1))[0]
        bands=[]; 
        for r in rows:
            if bands and r-bands[-1][1]<=3: bands[-1][1]=r
            else: bands.append([r,r])
        aligned=np.full_like(labels,255)
        for r0,r1 in bands:
            band=labels[r0:r1+1]; cols=np.nonzero((band<128).any(axis=0))[0]
            if len(cols)==0: continue
            shift=cols.min()-30; aligned[r0:r1+1, :XB-shift]=band[:, shift:] if shift>0 else band
        out=np.concatenate([treepart,aligned],axis=1)
    Image.fromarray(out).save(dst); print("saved",dst,out.shape)

main()
