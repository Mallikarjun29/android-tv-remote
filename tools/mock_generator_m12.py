from PIL import Image, ImageDraw, ImageFont
W,H=1080,2200
F="/usr/share/fonts/truetype/noto/NotoSans-%s.ttf"
def f(s,b=False): return ImageFont.truetype(F%("Bold" if b else "Regular"),s)
BG=(15,18,28);CARD=(28,33,50);ACC=(94,140,255);TX=(236,240,250);MU=(178,188,212);DK=(10,14,30)
def base():
    im=Image.new("RGB",(W,H),BG);d=ImageDraw.Draw(im)
    d.text((60,40),"9:41",font=f(34,True),fill=TX);d.text((W-200,40),"5G  100%",font=f(30),fill=TX)
    return im,d
def ctr(d,x,y,t,fo,fill):
    w=d.textlength(t,font=fo);d.text((x-w/2,y),t,font=fo,fill=fill)
def header(d,title,sub,online=False):
    d.text((60,130),title,font=f(54,True),fill=TX)
    if online:
        d.ellipse((62,218,86,242),fill=(110,220,150));d.text((106,205),sub,font=f(32),fill=(110,220,150))
    else: d.text((60,205),sub,font=f(32),fill=MU)
def power(d):
    x=W-125;d.ellipse((x-45,120,x+45,210),fill=(60,30,40))
    d.arc((x-25,140,x+25,190),-60,240,fill=(255,120,130),width=6);d.line((x,135,x,165),fill=(255,120,130),width=6)
# icons
def ic_back(d,x,y,c=TX,k=30):
    d.polygon([(x-k,y),(x,y-k),(x,y-k//3),(x+k,y-k//3),(x+k,y+k//3),(x,y+k//3),(x,y+k)],fill=c)
def ic_home(d,x,y,c=TX,k=32):
    d.polygon([(x-k,y),(x,y-k),(x+k,y)],fill=c);d.rectangle((x-k+8,y,x+k-8,y+k),fill=c);d.rectangle((x-8,y+8,x+8,y+k),fill=CARD)
def ic_menu(d,x,y,c=TX,k=30):
    for dy in (-18,0,18): d.rounded_rectangle((x-k,y+dy-4,x+k,y+dy+4),4,fill=c)
def ic_plus(d,x,y,c=TX,k=24):
    d.rounded_rectangle((x-k,y-4,x+k,y+4),4,fill=c);d.rounded_rectangle((x-4,y-k,x+4,y+k),4,fill=c)
def ic_minus(d,x,y,c=TX,k=24): d.rounded_rectangle((x-k,y-4,x+k,y+4),4,fill=c)
def ic_spk(d,x,y,c=TX):
    d.polygon([(x-30,y-12),(x-14,y-12),(x+6,y-30),(x+6,y+30),(x-14,y+12),(x-30,y+12)],fill=c);d.arc((x-4,y-18,x+26,y+18),-50,50,fill=c,width=5);d.arc((x+8,y-30,x+42,y+30),-50,50,fill=c,width=5)
def ic_mute(d,x,y,c=TX):
    ic_spk(d,x,y,c);d.line((x-34,y+30,x+40,y-30),fill=(255,120,130),width=6)
def ic_play(d,x,y,c=TX):
    d.polygon([(x-22,y-28),(x-22,y+28),(x+4,y)],fill=c);d.rectangle((x+10,y-26,x+16,y+26),fill=c);d.rectangle((x+24,y-26,x+30,y+26),fill=c)
def rbtn(d,x,y,w,h,icon,fill=CARD):
    d.rounded_rectangle((x,y,x+w,y+h),h//2,fill=fill);icon(d,x+w//2,y+h//2)
def tabs(d,a):
    d.rectangle((0,H-190,W,H),fill=(22,26,40))
    xs=[180,540,900];names=["Remote","Apps","Keyboard"]
    for i,x in enumerate(xs):
        c=ACC if i==a else MU;y=H-135
        if i==0: d.rounded_rectangle((x-18,y-34,x+18,y+34),8,outline=c,width=5);d.ellipse((x-6,y-18,x+6,y-6),fill=c)
        if i==1:
            for ax in (-30,4):
                for ay in (-30,4): d.rounded_rectangle((x+ax,y+ay,x+ax+26,y+ay+26),6,outline=c,width=5)
        if i==2:
            d.rounded_rectangle((x-36,y-24,x+36,y+24),8,outline=c,width=5)
            for kx in (-22,-8,6,20): d.rectangle((x+kx,y-10,x+kx+6,y-4),fill=c)
            d.rectangle((x-20,y+6,x+20,y+12),fill=c)
        ctr(d,x,H-92,names[i],f(28,i==a),c)
def pill(d,x,y,w,h,t,fill=CARD,fc=TX,s=36):
    d.rounded_rectangle((x,y,x+w,y+h),h//2,fill=fill);ctr(d,x+w/2,y+h/2-s*0.75,t,f(s,True),fc)
def toggle(d,on):
    x,y=60,260;d.text((x,y+14),"Touch mode",font=f(36,True),fill=TX)
    tx=330;d.rounded_rectangle((tx,y+8,tx+120,y+68),30,fill=ACC if on else (55,62,88))
    cx=tx+90 if on else tx+30;d.ellipse((cx-24,y+14,cx+24,y+62),fill=(255,255,255))
    d.text((tx+150,y+18),"ON" if on else "OFF",font=f(30,True),fill=ACC if on else MU)
def dots(d,active,y=1790):
    for i in range(2): d.ellipse((540-30+i*40-8,y-8,540-30+i*40+8,y+8),fill=ACC if i==active else (60,66,90))
def dpad(d):
    cx,cy,R=540,800,310
    d.ellipse((cx-R,cy-R,cx+R,cy+R),fill=CARD);d.ellipse((cx-125,cy-125,cx+125,cy+125),fill=ACC)
    k=34
    for dx,dy in [(0,-225),(0,225),(-225,0),(225,0)]:
        X,Y=cx+dx,cy+dy
        pts={(0,-225):[(X-k,Y+k//2),(X+k,Y+k//2),(X,Y-k)],(0,225):[(X-k,Y-k//2),(X+k,Y-k//2),(X,Y+k)],(-225,0):[(X+k//2,Y-k),(X+k//2,Y+k),(X-k,Y)],(225,0):[(X-k//2,Y-k),(X-k//2,Y+k),(X+k,Y)]}[(dx,dy)]
        d.polygon(pts,fill=TX)
def lower(d):
    for i,ic in enumerate([ic_back,ic_home,ic_menu]): rbtn(d,95+i*310,1210,290,120,ic)
    # vol
    d.rounded_rectangle((95,1390,285,1740),95,fill=CARD);ic_plus(d,190,1470);ic_spk(d,175,1565);ic_minus(d,190,1670)
    rbtn(d,445,1430,190,190,ic_mute) if False else None
    d.ellipse((445,1400,635,1590),fill=CARD);ic_mute(d,540,1495);ctr(d,540,1610,"Mute",f(28),MU)
    d.rounded_rectangle((795,1390,985,1740),95,fill=CARD);ctr(d,890,1450,"CH +",f(36,True),TX);ctr(d,890,1620,"CH –",f(36,True),TX)
    d.ellipse((445,1640,635,1740),fill=CARD) if False else None
GRN=(110,220,150);RED=(255,120,130)
def tabs3(d,a,enabled=True):
    d.rectangle((0,H-190,W,H),fill=(22,26,40))
    xs=[180,540,900];names=["Remote","Apps","Keyboard"]
    for i,x in enumerate(xs):
        c=ACC if (i==a and enabled) else (MU if enabled else (85,92,115));y=H-135
        if i==0: d.rounded_rectangle((x-18,y-34,x+18,y+34),8,outline=c,width=5);d.ellipse((x-6,y-18,x+6,y-6),fill=c)
        if i==1:
            for ax in (-30,4):
                for ay in (-30,4): d.rounded_rectangle((x+ax,y+ay,x+ax+26,y+ay+26),6,outline=c,width=5)
        if i==2:
            d.rounded_rectangle((x-36,y-24,x+36,y+24),8,outline=c,width=5)
            for kx in (-22,-8,6,20): d.rectangle((x+kx,y-10,x+kx+6,y-4),fill=c)
            d.rectangle((x-20,y+6,x+20,y+12),fill=c)
        ctr(d,x,H-92,names[i],f(28,i==a and enabled),c)
def rhead(d,sub="Connected"):
    d.text((60,120),"Living Room Sony Bravia 5…",font=f(46,True),fill=TX)
    d.ellipse((62,206,86,230),fill=GRN);d.text((100,192),sub,font=f(30),fill=GRN)
    d.rounded_rectangle((420,190,580,240),25,outline=MU,width=3);ctr(d,500,196,"Switch",f(28,True),MU)
    power(d)
def touchtoggle(d,on):
    y=270;d.text((60,y+10),"Touch mode",font=f(34,True),fill=TX)
    tx=300;d.rounded_rectangle((tx,y+6,tx+110,y+62),28,fill=ACC if on else (70,78,105))
    cx=tx+82 if on else tx+28;d.ellipse((cx-22,y+12,cx+22,y+56),fill=(255,255,255))
def chip(d,x,y,w,h,t,fill=CARD,fc=TX,s=32,out=None):
    d.rounded_rectangle((x,y,x+w,y+h),h//2,fill=fill,outline=out,width=3 if out else 0);ctr(d,x+w/2,y+h/2-s*0.78,t,f(s,True),fc)
def navrow(d,pressed=None):
    y=1440
    for i,ic in enumerate([ic_back,ic_home,ic_menu]):
        x=60+i*320;fill=(60,86,160) if pressed==i else CARD
        d.rounded_rectangle((x,y,x+300,y+145),72,fill=fill);ic(d,x+150,y+72,TX,30)
def volrow(d,muted=False):
    y0,y1=1625,1990
    for x0,xc,lab in ((60,175,"VOL"),(790,905,"CH")):
        d.rounded_rectangle((x0,y0,x0+230,y1),100,fill=CARD)
        ctr(d,xc,y0+30,lab,f(32,True),MU);ic_plus(d,xc,y0+112,TX,27)
        ic_minus(d,xc,y1-118,TX,27);ctr(d,xc,y1-78,lab,f(32,True),MU)
    d.ellipse((435,1655,645,1865),fill=CARD);ic_mute(d,540,1735);ctr(d,540,1780,"Mute",f(30),MU)
    ctr(d,540,1900,"Volume 18",f(28),MU)
    if muted: ctr(d,540,1940,"Muted",f(26,True),RED)
def dots3(d,active,y=1540):
    for i in range(2): d.ellipse((520+i*40-8,y-8,520+i*40+8,y+8),fill=ACC if i==active else (70,76,100))
# 1 pairing step
im,d=base();header(d,"Add your TV","Step 1 of 2 · Choose your TV")
for i,(n,ip,sel) in enumerate([("Living Room Sony Bravia 55","192.168.1.24",True),("Bedroom Chromecast","192.168.1.31",False)]):
    y=300+i*200;d.rounded_rectangle((60,y,W-60,y+170),36,fill=(30,44,84) if sel else CARD,outline=ACC if sel else None,width=4)
    d.rounded_rectangle((95,y+45,195,y+120),12,outline=ACC,width=6);d.line((125,y+135,165,y+135),fill=ACC,width=6)
    d.text((240,y+30),n[:24]+("…" if len(n)>24 else ""),font=f(38,True),fill=TX);d.text((240,y+90),ip,font=f(30),fill=MU)
    if sel: d.ellipse((W-170,y+55,W-100,y+125),fill=ACC);d.line((W-155,y+92,W-141,y+106),fill=DK,width=8);d.line((W-141,y+106,W-116,y+76),fill=DK,width=8)
chip(d,60,700,420,90,"Scan again",fc=ACC);chip(d,500,700,460,90,"TV not listed?",fc=MU,out=MU)
d.text((60,860),"Step 2 of 2 · Enter the code on your TV",font=f(34,True),fill=TX)
d.rounded_rectangle((60,930,W-60,1070),36,fill=CARD,outline=ACC,width=4);d.text((100,970),"A7K2M9",font=f(60,True),fill=TX)
chip(d,W-330,950,240,100,"Paste",fill=(40,60,120),fc=TX)
d.text((60,1100),"6 characters, shown on the TV screen. Pasting works.",font=f(28),fill=MU)
chip(d,60,1220,W-120,130,"Connect",fill=ACC,fc=DK,s=42)
d.rounded_rectangle((60,1450,W-60,1620),30,fill=(40,22,30));d.text((100,1480),"Wrong code",font=f(34,True),fill=RED);d.text((100,1535),"Check the code on the TV and try again.",font=f(28),fill=TX)
d.text((60,1740),"Remote, Apps and Keyboard unlock once a TV is connected.",font=f(28),fill=MU)
tabs3(d,0,False);im.save("/tmp/p1.png")
# 2 remote dpad
im,d=base();rhead(d);touchtoggle(d,False);chip(d,W-300,262,240,80,"123",fill=(40,60,120),s=34)
cx,cy,R=540,800,290
d.ellipse((cx-R,cy-R,cx+R,cy+R),fill=CARD);d.ellipse((cx-115,cy-115,cx+115,cy+115),fill=ACC)
k=34
for dx,dy in [(0,-205),(0,205),(-205,0),(205,0)]:
    X,Y=cx+dx,cy+dy
    pts={(0,-205):[(X-k,Y+k//2),(X+k,Y+k//2),(X,Y-k)],(0,205):[(X-k,Y-k//2),(X+k,Y-k//2),(X,Y+k)],(-205,0):[(X+k//2,Y-k),(X+k//2,Y+k),(X-k,Y)],(205,0):[(X-k//2,Y-k),(X-k//2,Y+k),(X+k,Y)]}[(dx,dy)]
    d.polygon(pts,fill=TX)
dots3(d,0,1395);ctr(d,540,1345,"Tap 123 or swipe the page for the keypad",f(26),MU)
navrow(d);volrow(d);tabs3(d,0);im.save("/tmp/p2.png")
# 3 touch
im,d=base();rhead(d);touchtoggle(d,True);chip(d,W-300,262,240,80,"123",fill=(40,60,120),s=34)
d.rounded_rectangle((60,380,W-60,1330),50,fill=CARD)
for gx in range(120,W-100,90):
    for gy in range(440,1300,90): d.ellipse((gx,gy,gx+6,gy+6),fill=(58,66,94))
ctr(d,540,820,"Swipe to move",f(54,True),MU);ctr(d,540,900,"Tap = OK",f(32),MU);ctr(d,540,950,"Page swipes are off in touch mode",f(26),MU)
navrow(d,pressed=1);volrow(d);tabs3(d,0);im.save("/tmp/p3.png")
# 4 keypad
im,d=base();rhead(d);d.text((60,280),"Number keypad",font=f(34,True),fill=MU);chip(d,W-300,262,240,80,"D-pad",fill=(40,60,120),s=34)
d.rounded_rectangle((60,370,W-60,450),30,fill=CARD,outline=ACC,width=3);d.text((100,375),"2 5 1|",font=f(48,True),fill=TX);d.text((W-340,392),"Channel",font=f(28),fill=MU)
keys=["1","2","3","4","5","6","7","8","9","bs","0","ok"]
for i,k_ in enumerate(keys):
    x=60+(i%3)*320;y=470+(i//3)*205;w,h=300,185
    if k_=="ok": d.rounded_rectangle((x,y,x+w,y+h),50,fill=ACC);ctr(d,x+150,y+55,"OK",f(70,True),DK);continue
    d.rounded_rectangle((x,y,x+w,y+h),50,fill=CARD)
    if k_=="bs":
        cx2,cy2=x+150,y+85;d.polygon([(cx2-50,cy2),(cx2-20,cy2-34),(cx2+50,cy2-34),(cx2+50,cy2+34),(cx2-20,cy2+34)],outline=TX,width=6);d.line((cx2-5,cy2-14,cx2+30,cy2+14),fill=TX,width=6);d.line((cx2-5,cy2+14,cx2+30,cy2-14),fill=TX,width=6);ctr(d,x+150,y+135,"Delete",f(28),MU)
    else: ctr(d,x+150,y+30,k_,f(84,True),TX)
dots3(d,1,1390);navrow(d);volrow(d);tabs3(d,0);im.save("/tmp/p4.png")
# 5 apps
im,d=base();header(d,"Apps","")
d.ellipse((62,222,86,246),fill=GRN);d.text((100,208),"Living Room Sony Bravia 5...",font=f(30),fill=GRN)
L={n:Image.open(f"/tmp/logos/{n}.png").convert("RGBA") for n in ["netflix","prime","apple","hotstar","zee5","xstream"]}
tiles=[("Airtel Xstream Play","xstream",(255,255,255)),("ZEE5","zee5",(255,255,255)),("JioHotstar","hotstar",(255,255,255)),("Netflix","netflix",(0,0,0)),("Prime Video","prime",(255,255,255)),("Apple TV+","apple",(255,255,255))]
for i,(n,kk,bg) in enumerate(tiles):
    x=60+(i%2)*500;y=290+(i//2)*400;d.rounded_rectangle((x,y,x+460,y+360),50,fill=CARD)
    d.rounded_rectangle((x+30,y+30,x+430,y+230),36,fill=bg)
    lg=L[kk].copy();lg.thumbnail((320,130) if kk!="netflix" else (130,130));im.paste(lg,(x+230-lg.width//2,y+130-lg.height//2),lg)
    if n=="Netflix":
        ctr(d,x+230,y+250,"Opening…",f(34,True),ACC);d.rounded_rectangle((x+60,y+312,x+400,y+324),6,fill=(55,62,88));d.rounded_rectangle((x+60,y+312,x+250,y+324),6,fill=ACC)
    else: ctr(d,x+230,y+262,n,f(36,True),TX)
# add tile (dashed)
x,y=60,1490
d.rounded_rectangle((x,y,x+460,y+360),50,fill=(20,24,38))
import math
def dashed_rr(d,box,r,col,w=4):
    x0,y0,x1,y1=box
    for xx in range(int(x0+r),int(x1-r),36):
        d.line((xx,y0,min(xx+20,x1-r),y0),fill=col,width=w);d.line((xx,y1,min(xx+20,x1-r),y1),fill=col,width=w)
    for yy in range(int(y0+r),int(y1-r),36):
        d.line((x0,yy,x0,min(yy+20,y1-r)),fill=col,width=w);d.line((x1,yy,x1,min(yy+20,y1-r)),fill=col,width=w)
    for (ax,ay,st) in ((x0,y0,180),(x1-2*r,y0,270),(x0,y1-2*r,90),(x1-2*r,y1-2*r,0)):
        d.arc((ax,ay,ax+2*r,ay+2*r),st,st+90,fill=col,width=w)
dashed_rr(d,(x,y,x+460,y+360),50,ACC)
d.ellipse((x+160,y+60,x+300,y+200),fill=(40,60,120));ic_plus(d,x+230,y+130,TX,40)
ctr(d,x+230,y+250,"Add custom app",f(34,True),ACC)
x,y=560,1490;d.rounded_rectangle((x,y,x+460,y+360),50,fill=CARD)
d.rounded_rectangle((x+30,y+30,x+430,y+230),36,fill=(40,60,120))
cx,cy=x+230,y+130;d.arc((cx-70,cy-28,cx+6,cy+28),90,270,fill=TX,width=12);d.arc((cx-6,cy-28,cx+70,cy+28),270,90,fill=TX,width=12);d.line((cx-30,cy,cx+30,cy),fill=TX,width=12)
ctr(d,x+230,y+262,"Cricket highlights",f(34,True),TX)
d.rounded_rectangle((60,1880,W-60,1985),30,fill=(40,22,30));d.text((95,1905),"Prime Video isn't installed on this TV",font=f(30,True),fill=RED);d.text((95,1945),"Install it from the TV's Play Store, then try again.",font=f(24),fill=TX)
tabs3(d,1);im.save("/tmp/p5.png")
# 6 keyboard
im,d=base();header(d,"Type on TV","")
d.ellipse((62,222,86,246),fill=GRN);d.text((100,208),"Living Room Sony Bravia 5…",font=f(30),fill=GRN)
d.rounded_rectangle((60,260,W-60,420),36,fill=(24,44,40));d.text((100,285),"Typing to: Netflix search",font=f(36,True),fill=GRN);d.text((100,350),"Sends live as you type",font=f(28),fill=MU)
d.rounded_rectangle((60,450,W-60,610),36,fill=CARD,outline=ACC,width=4);d.text((100,505),"Stranger Th|",font=f(44),fill=TX)
chip(d,60,640,330,80,"Auto-open: ON",fill=(40,60,120),s=28);chip(d,410,640,330,80,"Hide keyboard",fc=MU,out=MU,s=28);chip(d,760,640,260,80,"Send",fill=ACC,fc=DK,s=28)
d.text((60,760),"If the TV has no text field open, this shows a hint instead of typing.",font=f(26),fill=MU)
d.rectangle((0,1000,W,2010),fill=(32,36,52));d.text((60,1015),"Your phone's keyboard",font=f(26),fill=MU)
for r,row in enumerate(["QWERTYUIOP","ASDFGHJKL","ZXCVBNM"]):
    n=len(row);kw=88;gap=12;x0=(W-(n*kw+(n-1)*gap))/2+(0 if r<2 else 0)
    for i,c in enumerate(row):
        x=x0+i*(kw+gap)
        if r==2: x=x0+i*(kw+gap)
        y=1070+r*150;d.rounded_rectangle((x,y,x+kw,y+125),22,fill=CARD);ctr(d,x+kw/2,y+30,c,f(46,True),TX)
d.rounded_rectangle((60,1520,160,1645),22,fill=(48,54,76));d.polygon([(110,1545),(85,1580),(100,1580),(100,1620),(120,1620),(120,1580),(135,1580)],fill=TX)
x=W-160;d.rounded_rectangle((x,1520,x+100,1645),22,fill=(48,54,76))
cx2,cy2=x+50,1583;d.polygon([(cx2-30,cy2),(cx2-12,cy2-20),(cx2+30,cy2-20),(cx2+30,cy2+20),(cx2-12,cy2+20)],outline=TX,width=4)
pill(d,60,1690,200,120,"123",s=34);pill(d,280,1690,520,120,"space",s=34);pill(d,820,1690,200,120,"Search",fill=ACC,fc=DK,s=32)
tabs3(d,2);im.save("/tmp/p6.png")
# 7 no TV found
im,d=base();header(d,"Add your TV","No TV found")
d.rounded_rectangle((60,300,W-60,500),36,fill=(40,22,30));d.text((100,335),"Scanned for 10 seconds, nothing found.",font=f(34,True),fill=RED)
d.text((100,405),"Try these, then scan again:",font=f(30),fill=TX)
for i,t in enumerate(["Phone and TV are on the same Wi-Fi","TV is on and not in deep standby","Turn off any VPN on this phone","Enter the TV's IP address manually"]):
    y=560+i*140;d.rounded_rectangle((60,y,W-60,y+110),30,fill=CARD);d.ellipse((90,y+30,140,y+80),outline=ACC,width=5);ctr(d,115,y+34,str(i+1),f(34,True),ACC);d.text((175,y+30),t,font=f(32),fill=TX)
chip(d,60,1180,W-120,130,"Scan again",fill=ACC,fc=DK,s=42)
chip(d,60,1340,W-120,110,"Enter IP address",fc=ACC,out=ACC,s=36)
tabs3(d,0,False);im.save("/tmp/p7.png")
# 8/9 picker
def seg(d,mode):
    d.rounded_rectangle((100,330,W-100,410),40,fill=(20,24,38))
    xs=[(104,540),(540,W-104)]
    for i,(x0,x1) in enumerate(xs):
        on=(i==mode);
        if on: d.rounded_rectangle((x0,334,x1,406),36,fill=ACC)
        ctr(d,(x0+x1)/2,348,["App","Link"][i],f(34,True),DK if on else MU)
def field(d,y,ph,paste=False):
    d.rounded_rectangle((100,y,W-100,y+95),30,fill=(20,24,38),outline=(70,78,105),width=3);d.text((130,y+23),ph,font=f(32),fill=MU)
    if paste: chip(d,W-330,y+12,210,70,"Paste",fill=(40,60,120),s=28)
def picker(mode,out):
    im,d=base();header(d,"Add custom app","Build your own app row")
    d.rounded_rectangle((60,290,W-60,770 if mode==1 else 780),40,fill=CARD)
    d.text((100,296),"",font=f(10),fill=TX);seg(d,mode)
    if mode==0:
        field(d,440,"App name");field(d,560,"Package name");chip(d,100,680,W-200,80,"Add to my apps",fill=ACC,fc=DK,s=32)
        d.text((60,810),"Or pick from suggestions",font=f(32,True),fill=MU)
        d.rounded_rectangle((60,865,W-60,965),50,fill=CARD);d.ellipse((100,895,134,929),outline=MU,width=5);d.line((130,925,148,943),fill=MU,width=5);d.text((175,890),"Search suggestions",font=f(32),fill=MU)
        rows=[("YouTube",False,(200,60,60)),("SonyLIV",False,(60,70,120)),("Hoichoi",False,(150,40,60)),("Spotify",False,(40,150,90))]
        for i,(n,added,c) in enumerate(rows):
            y=995+i*190;d.rounded_rectangle((60,y,W-60,y+165),36,fill=CARD)
            d.rounded_rectangle((95,y+30,200,y+135),26,fill=c);ctr(d,147,y+55,n[0],f(60,True),(255,255,255))
            d.text((235,y+50),n,font=f(38,True),fill=TX);chip(d,W-330,y+40,240,85,"Add",fill=ACC,fc=DK,s=32)
    else:
        field(d,440,"Name (e.g. Cricket highlights)");field(d,560,"Paste link",paste=True);chip(d,100,680,W-200,80,"Add link",fill=ACC,fc=DK,s=32)
        d.text((60,810),"Links open on the TV when tapped",font=f(32,True),fill=MU)
        for i,(t,sb) in enumerate([("YouTube video or channel","youtube.com/..."),("Web page","https://..."),("App link","app://... or intent link")]):
            y=870+i*185;d.rounded_rectangle((60,y,W-60,y+160),36,fill=CARD)
            cx=147;cy=y+80;d.ellipse((cx-45,cy-45,cx+45,cy+45),fill=(40,60,120))
            d.arc((cx-28,cy-14,cx+4,cy+14),90,270,fill=TX,width=7);d.arc((cx-4,cy-14,cx+28,cy+14),270,90,fill=TX,width=7);d.line((cx-14,cy,cx+14,cy),fill=TX,width=7)
            d.text((235,y+28),t,font=f(36,True),fill=TX);d.text((235,y+90),sb,font=f(30),fill=MU)
        d.text((60,1470),"Tip: share a link from your phone straight to this app.",font=f(28),fill=MU)
    tabs3(d,1);im.save(out)
picker(0,"/tmp/p8.png");picker(1,"/tmp/p9.png")
ims=[Image.open(f"/tmp/p{i}.png").resize((360,733)) for i in range(1,10)]
c=Image.new("RGB",(5*360+6*30,2*733+90),(8,10,18))
for i,m in enumerate(ims): c.paste(m,(30+(i%5)*390,30+(i//5)*763))
c.save("/tmp/pall.png")
