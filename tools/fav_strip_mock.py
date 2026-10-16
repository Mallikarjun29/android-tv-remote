# Mock: favourites strip shared by the D-pad page and the keypad page (bigger tiles). Design proposal drawn with PIL, not a real screenshot.
import os
HERE=os.path.dirname(os.path.abspath(__file__))
exec(open(os.path.join(HERE,"mock_generator.py")).read().split("# 1 pairing step")[0])
exec(open(os.path.join(HERE,"fav_channels_mock.py")).read().split("# 1 empty state")[0])
SH=152  # tile height in px (58dp); was 112 (46dp)
def tile(d,x,y,w,name,num):
    d.rounded_rectangle((x,y,x+w,y+SH),50,fill=CARD)
    ctr(d,x+w/2,y+26,name,f(40,True),TX);ctr(d,x+w/2,y+88,num,f(32),MU)
def strip(d,favs,y=380):
    x=60
    for n,nm in favs:
        w=max(250,int(d.textlength(n,font=f(40,True)))+110)
        if x+w>W-30:
            tile(d,x,y,w,n,nm);d.rectangle((W-80,y-6,W,y+SH+6),fill=BG);return
        tile(d,x,y,w,n,nm);x+=w+22
    dashed_rr(d,(x,y,x+SH,y+SH),50,ACC);ic_plus(d,x+SH//2,y+SH//2,ACC,24)
def dpad_page(out):
    im,d=base();rhead(d)
    d.text((60,280),"Touch mode",font=f(36,True),fill=TX)
    tx=330;d.rounded_rectangle((tx,274,tx+120,334),30,fill=(55,62,88));d.ellipse((tx+6,280,tx+54,328),fill=(255,255,255))
    chip(d,W-300,262,240,80,"123",fill=(40,60,120),s=34)
    strip(d,FAVS)
    cx,cy,R=540,980,300
    d.ellipse((cx-R,cy-R,cx+R,cy+R),fill=CARD);d.ellipse((cx-120,cy-120,cx+120,cy+120),fill=ACC)
    k=34
    for dx,dy in [(0,-215),(0,215),(-215,0),(215,0)]:
        X,Y=cx+dx,cy+dy
        pts={(0,-215):[(X-k,Y+k//2),(X+k,Y+k//2),(X,Y-k)],(0,215):[(X-k,Y-k//2),(X+k,Y-k//2),(X,Y+k)],(-215,0):[(X+k//2,Y-k),(X+k//2,Y+k),(X-k,Y)],(215,0):[(X-k//2,Y-k),(X-k//2,Y+k),(X+k,Y)]}[(dx,dy)]
        d.polygon(pts,fill=TX)
    dots3(d,0,1560);bottom_block(d);tabs3(d,0);im.save(out)
def keypad_page(out):
    im,d=base();rhead(d)
    d.text((60,280),"Touch mode",font=f(36,True),fill=TX)
    tx=330;d.rounded_rectangle((tx,274,tx+120,334),30,fill=(55,62,88));d.ellipse((tx+6,280,tx+54,328),fill=(255,255,255))
    chip(d,W-300,262,240,80,"D-pad",fill=(40,60,120),s=34)
    strip(d,FAVS)
    d.text((W//2-30,570),"",font=f(40),fill=TX)
    keys(d,590);dots3(d,1,1560);bottom_block(d);tabs3(d,0);im.save(out)
dpad_page("/tmp/fav2/fav-strip-dpad.png");keypad_page("/tmp/fav2/fav-strip-keypad.png")
from PIL import Image
ims=[Image.open(f"/tmp/fav2/fav-strip-{n}.png").resize((540,1100)) for n in ("dpad","keypad")]
c=Image.new("RGB",(1120,1100),(40,44,58));c.paste(ims[0],(10,0));c.paste(ims[1],(570,0));c.save("/tmp/fav2/fav-strip-overview.png")
