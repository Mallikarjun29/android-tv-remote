# Mock: favourite channels on the keypad page (design proposal, not built). Reuses the helpers of mock_generator.py.
import os
src=open(os.path.join(os.path.dirname(os.path.abspath(__file__)),"mock_generator.py")).read().split("# 1 pairing step")[0]
exec(src)
def dashed_rr(d,box,r,col,w=4):
    x0,y0,x1,y1=box
    for xx in range(int(x0+r),int(x1-r),36):
        d.line((xx,y0,min(xx+20,x1-r),y0),fill=col,width=w);d.line((xx,y1,min(xx+20,x1-r),y1),fill=col,width=w)
    for yy in range(int(y0+r),int(y1-r),36):
        d.line((x0,yy,x0,min(yy+20,y1-r)),fill=col,width=w);d.line((x1,yy,x1,min(yy+20,y1-r)),fill=col,width=w)
    for (ax,ay,st) in ((x0,y0,180),(x1-2*r,y0,270),(x0,y1-2*r,90),(x1-2*r,y1-2*r,0)):
        d.arc((ax,ay,ax+2*r,ay+2*r),st,st+90,fill=col,width=w)

def bottom_block(d):
    y0,y1=1640,1990
    for x0,xc,lab in ((60,175,"VOL"),(790,905,"CH")):
        d.rounded_rectangle((x0,y0,x0+230,y1),100,fill=CARD)
        ic_plus(d,xc,y0+70,TX,27);ctr(d,xc,y0+135,lab,f(32,True),MU)
        if lab=="VOL": ctr(d,xc,y0+185,"18",f(34,True),TX)
        ic_minus(d,xc,y1-70,TX,27)
    for i,ic in enumerate([ic_back,ic_home]):
        x=310+i*235;d.rounded_rectangle((x,1640,x+215,1770),65,fill=CARD);ic(d,x+107,1705,TX,30)
    for i,kind in enumerate(("tv","mute")):
        cx=400+i*140;cy=1880
        d.ellipse((cx-62,cy-62,cx+62,cy+62),fill=CARD)
        if kind=="tv":
            d.rounded_rectangle((cx-30,cy-24,cx+30,cy+16),8,outline=TX,width=6);d.line((cx-14,cy+30,cx+14,cy+30),fill=TX,width=6)
        else: ic_spk(d,cx-6,cy,TX)

def keys(d,y0):
    ks=["1","2","3","4","5","6","7","8","9","bs","0","ok"]
    for i,k_ in enumerate(ks):
        x=60+(i%3)*320;y=y0+(i//3)*185;w,h=300,165
        if k_=="ok": d.rounded_rectangle((x,y,x+w,y+h),48,fill=ACC);ctr(d,x+150,y+40,"OK",f(64,True),DK);continue
        d.rounded_rectangle((x,y,x+w,y+h),48,fill=CARD)
        if k_=="bs":
            cx2,cy2=x+150,y+72;d.polygon([(cx2-46,cy2),(cx2-18,cy2-30),(cx2+46,cy2-30),(cx2+46,cy2+30),(cx2-18,cy2+30)],outline=TX,width=6)
            d.line((cx2-4,cy2-12,cx2+28,cy2+12),fill=TX,width=6);d.line((cx2-4,cy2+12,cx2+28,cy2-12),fill=TX,width=6);ctr(d,x+150,y+112,"Delete",f(26),MU)
        else: ctr(d,x+150,y+26,k_,f(78,True),TX)

FAVS=[("Star Plus","101"),("Zee TV","102"),("Colors","103"),("Sony SET","104"),("Star Sports 1","201"),("Aaj Tak","301")]

def chip2(d,x,y,w,h,name,num,sel=False):
    d.rounded_rectangle((x,y,x+w,y+h),38,fill=(40,60,120) if sel else CARD,outline=ACC if sel else None,width=3 if sel else 0)
    ctr(d,x+w/2,y+18,name,f(30,True),TX);ctr(d,x+w/2,y+64,num,f(26),MU)

def page(favs,out,pressed=None,typed="",hint=None):
    im,d=base();rhead(d)
    d.text((60,280),"Number keypad",font=f(34,True),fill=MU);chip(d,W-300,262,240,80,"D-pad",fill=(40,60,120),s=34)
    d.text((60,370),"Favourite channels",font=f(30,True),fill=MU)
    if favs:
        d.text((W-330,374),"Hold a chip to edit",font=f(26),fill=(120,130,160))
    # chip row (scrolls sideways)
    y=420;h=112
    if not favs:
        dashed_rr(d,(60,y,W-60,y+h),40,ACC)
        d.ellipse((92,y+26,152,y+86),fill=(40,60,120));ic_plus(d,122,y+56,TX,15)
        d.text((176,y+14),"Add your first favourite",font=f(32,True),fill=ACC)
        d.text((176,y+60),"Type a number below, then hold here to save it",font=f(25),fill=MU)
    else:
        x=60
        for i,(n,nm) in enumerate(favs):
            w=250 if len(n)<10 else 300
            if x+w>W-40: 
                # partly visible chip: fade at the edge
                chip2(d,x,y,w,h,n,nm);d.rectangle((W-90,y-6,W,y+h+6),fill=BG);break
            chip2(d,x,y,w,h,n,nm,sel=(pressed==i));x+=w+18
        else:
            dashed_rr(d,(x,y,x+110,y+h),40,ACC);ic_plus(d,x+55,y+56,ACC,20)
    # echo
    d.rounded_rectangle((60,560,W-60,640),30,fill=CARD,outline=ACC,width=3)
    d.text((100,565),(typed or "")+"|",font=f(48,True),fill=TX);d.text((W-340,582),"Channel",font=f(28),fill=MU)
    keys(d,670);dots3(d,1,1460);bottom_block(d);tabs3(d,0);return im,d

# 1 empty state
im,d=page([],"");im.save("/tmp/fav/fav-1-empty.png")
# 2 filled
im,d=page(FAVS,"");im.save("/tmp/fav/fav-2-filled.png")
# 3 long press -> save sheet
im,d=page(FAVS[:4],"",typed="105")
ov=Image.new("RGBA",(W,H),(0,0,0,150));im.paste(ov,(0,0),ov);d=ImageDraw.Draw(im)
d.rounded_rectangle((50,780,W-50,1500),60,fill=(24,29,46),outline=(60,68,98),width=3)
d.text((100,820),"Save favourite",font=f(46,True),fill=TX)
d.text((100,890),"Shown as a chip on this page",font=f(28),fill=MU)
d.text((100,970),"Name",font=f(28,True),fill=MU);d.rounded_rectangle((100,1010,W-100,1100),30,fill=CARD,outline=ACC,width=3);d.text((130,1022),"Sony Max|",font=f(40),fill=TX)
d.text((100,1140),"Channel number",font=f(28,True),fill=MU);d.rounded_rectangle((100,1180,W-100,1270),30,fill=CARD);d.text((130,1192),"105",font=f(40),fill=TX)
d.text((100,1285),"Sent as digits, then OK, when you tap the chip",font=f(26),fill=MU)
d.rounded_rectangle((100,1350,500,1450),50,outline=MU,width=3);ctr(d,300,1372,"Cancel",f(36,True),MU)
d.rounded_rectangle((540,1350,W-100,1450),50,fill=ACC);ctr(d,(540+W-100)/2,1372,"Save",f(36,True),DK)
im.save("/tmp/fav/fav-3-save-sheet.png")
# overview
ims=[Image.open(f"/tmp/fav/fav-{n}.png").resize((540,1100)) for n in ("1-empty","2-filled","3-save-sheet")]
c=Image.new("RGB",(3*540+4*30,1100+60),(8,10,18))
for i,m in enumerate(ims): c.paste(m,(30+i*570,30))
c.save("/tmp/fav/fav-overview.png")
