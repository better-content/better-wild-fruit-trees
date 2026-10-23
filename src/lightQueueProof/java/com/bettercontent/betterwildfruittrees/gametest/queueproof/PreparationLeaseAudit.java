package com.bettercontent.betterwildfruittrees.gametest.queueproof;
import java.util.List;
/** COLD primitive identity masks, same ordered footprint as the actual preparation lease. */
public final class PreparationLeaseAudit {
 final int scope;final List<QueueClearPolicy.Tile> tiles;final long full;
 long before,added,ready,readyForced,after;boolean beforeKnown,readyKnown,afterKnown,incomplete;
 public PreparationLeaseAudit(int scope,List<QueueClearPolicy.Tile> tiles){if(tiles.isEmpty()||tiles.size()>64)throw new IllegalArgumentException("audit footprint");this.scope=scope;this.tiles=tiles;full=tiles.size()==64?-1L:(1L<<tiles.size())-1;}
 static long key(QueueClearPolicy.Tile tile){return Integer.toUnsignedLong(tile.x())|((long)tile.z()<<32);}
 long bit(QueueClearPolicy.Tile tile){int at=tiles.indexOf(tile);if(at<0){incomplete=true;return 0;}return 1L<<at;}
 void attempted(QueueClearPolicy.Tile tile){long bit=bit(tile);if((added&bit)!=0)incomplete=true;added|=bit;}
 void ready(long key){for(int i=0;i<tiles.size();i++)if(key(tiles.get(i))==key){long bit=1L<<i;if((ready&bit)!=0)incomplete=true;ready|=bit;return;}incomplete=true;}
 public void entry(int x0,int y0,int z0,int x1,int y1,int z1){try{StringBuilder keys=new StringBuilder();for(int i=0;i<tiles.size();i++){if(i>0)keys.append(',');keys.append(i).append(':').append(Long.toHexString(key(tiles.get(i))));}PreparationAudit.record("ENTRY scope="+scope+" bounds="+x0+","+y0+","+z0+":"+x1+","+y1+","+z1+" inflated="+((long)x0-2)+","+((long)y0-3)+","+((long)z0-3)+":"+((long)x1+3)+","+((long)y1+20)+","+((long)z1+3)+" chunks="+tiles.size()+" keys="+keys);}catch(Throwable ignored){incomplete=true;}}
 void row(String phase){try{PreparationAudit.record("LEASE scope="+scope+" phase="+phase+" chunks="+tiles.size()+" fullMask="+Long.toHexString(full)+" beforeKnown="+beforeKnown+" before="+Long.toHexString(before)+" addedAttemptMask="+Long.toHexString(added)+" tickingReady="+Long.toHexString(ready)+" readyKnown="+readyKnown+" readyForced="+Long.toHexString(readyForced)+" afterKnown="+afterKnown+" after="+Long.toHexString(after)+" incomplete="+incomplete);}catch(Throwable ignored){incomplete=true;}}
}
