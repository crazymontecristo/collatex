/*
 * NMerge is Copyright 2009-2011 Desmond Schmidt
 *
 * This file is part of NMerge. NMerge is a Java library for merging
 * multiple versions into multi-version documents (MVDs), and for
 * reading, searching and comparing them.
 *
 * NMerge is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.edu.uq.nmerge.mvd;
import java.nio.charset.Charset;
import java.util.BitSet;
import java.util.Vector;
/**
 * Store and create matches for an MVD. Matches are runs within 
 * the whole MVD and may have been generated by a search or comparison 
 * etc. Matches, like chunks, designate runs that are to be treated 
 * specially, e.g. coloured differently. However, unlike chunks, they 
 * don't normally cover the whole version.
 */
public class Match extends BracketedData
{
	/** offset within the version where the match starts */
	int offset;
	/** length of the match */
	int length;
	/** id of the match version */
	short version;
	/** used in toString for formatting the match */
	String shortName;
	/** a flag to assist comparison with other matches */
	boolean found;
	/** what the status of the match is, what it represents */
	ChunkState state;
	/**
	 *	@param version the version in which this match occurs
	 *	@param offset the index within the version where the match starts
	 *	@param length the length of the match
	 *	@param shortName short name of the version
	 *	@param state the state of the matched text
	 */
	Match( short version, int offset, int length, String shortName, 
		ChunkState state )
	{
		super( Charset.defaultCharset().toString() );
		this.offset = offset;
		this.shortName = shortName;
		this.length = length;
		this.version = version;
		this.state = state;
	}
	/**
	 * Create a Match. In this version of Match we just store 
	 * the offset and length in a certain file.
	 * @param offset offset in bytes from the start of the file
	 * @param length length of the match
	 * @param version the version it belongs to
	 * @param shortName the short name (siglum) of the version
	 */
	public Match( int offset, int length, short version, 
		String shortName )
	{
		super( Charset.defaultCharset().toString() );
		this.length = length;
		this.offset = offset;
		this.version = version;
		this.shortName = shortName;
		this.state = ChunkState.none;
	}
	/**
	 * Create a Match by parsing its textual representation
	 * @param matchData the match data for several matches
	 * @param pos the offset within matchData to start reading
	 */
	public Match( byte[] matchData, int pos )
	{
		super( Charset.defaultCharset().toString() );
		int start = pos;
		while ( matchData[pos] != '[' )
			pos++;
		// point to first char after '['
		pos++;
		pos += readShortName( matchData, pos );
		pos += readVersion( matchData, pos );
		pos += readOffset( matchData, pos );
		pos += readLength( matchData, pos );
		srcLen = pos - start;
		this.state = ChunkState.none;
	}
	/**
	 * Read the length of the match
	 * @param matchData the data from the matches
	 * @param pos the start position for the length
	 * @return the number of bytes read
	 */
	private int readLength( byte[] matchData, int pos )
	{
		int start = pos;
		int begin = readTextLabel( matchData, pos );
		int len = readDigitData( matchData, begin );
		length = Integer.parseInt( new String(matchData,begin,len) );
		return (begin+len+1)-start;
	}
	/**
	 * Scan over a number
	 * @param matchData the matchdata to parse
	 * @param pos starting offset within matchData
	 * @return the number of bytes scanned over
	 */
	private int readDigitData( byte[] matchData, int pos )
	{
		int start = pos;
		while ( Character.isDigit((char)matchData[pos]) )
			pos++;
		return pos - start;
	}
	/**
	 * read the text label preceding a number
	 * @param matchData the matchdata to parse
	 * @param pos starting offset within matchData
	 * @return the updated offset after the text label
	 */
	private int readTextLabel( byte[] matchData, int pos )
	{
		while ( !Character.isDigit((char)matchData[pos]) )
			pos++;
		return pos;
	}
	/**
	 * Read the offset
	 * @param matchData the data from the matches
	 * @param pos the start position for the offset
	 * @return the number of bytes read
	 */
	private int readOffset( byte[] matchData, int pos )
	{
		int start = pos;
		int begin = readTextLabel( matchData, pos );
		int len = readDigitData( matchData, begin );
		offset = Integer.parseInt( new String(matchData,begin,len) );
		return (begin+len+1)-start;
	}
	/**
	 * Read the version
	 * @param matchData the data from the matches
	 * @param pos the start position for the version
	 * @return the number of bytes read
	 */
	private int readVersion( byte[] matchData, int pos )
	{
		int start = pos;
		int begin = readTextLabel( matchData, pos );
		int len = readDigitData( matchData, begin );
		version = Short.parseShort( new String(matchData,begin,len) );
		return (begin+len+1)-start;
	}
	/**
	 * Read and set the short name
	 * @param matchData the string representation of the matches
	 * @param pos the start offset within matchData
	 * @return the number of bytes read in the source data
	 */
	private int readShortName( byte[] matchData, int pos )
	{
		int start = pos;
		while ( matchData[pos] != ':' )
			pos++;
		int len = pos - start;
		shortName = new String( matchData, start, len );
		return len+1;
	}
	/**
	 *	Generate a match or a list of Match objects. This is called 
	 *	by search when a match is found that terminates in some pair.
	 *	@param len the length of the match
	 *	@param versions the versions in which to build the match
	 *	@param pairs the Vector of pairs from the MVD
	 *	@param endPair the last Pair in which the match occurs
	 *	@param endIndex the offset within the endPair in which 
	 *	the match occurs.
	 *	@param multiple true if multiple matches are desired
	 *	@return a list of Match objects
	 */
	static Match[] makeMatches( int len, BitSet versions, 
		MVD mvd, int endPair, int endIndex, 
		boolean multiple, ChunkState state ) throws Exception
	{
		BitSet bs = versions;
		Vector<Match> matches = new Vector<Match>();
		for ( int i=bs.nextSetBit(0); i>=0; i=bs.nextSetBit(i+1) ) 
		{
			// start from one byte after the match
			int offset = endIndex+1;
			// and add on the length of all relevant 
			// pairs up to the first one
			for ( int j=endPair-1;j>=0;j-- )
			{
				Pair p = mvd.pairs.get( j );
				if ( p.contains((short)i) )
				{
					offset += p.length();
				}
			}
			// now offset is len plus the real offset
			offset -= len;
			String shortName = mvd.getVersionShortName( i );
			Match m = new Match( (short)i, offset, len, shortName, 
				state );
			matches.add( m );
			if ( !multiple )
				break;
		}
		Match[] result = new Match[matches.size()];
		matches.toArray( result );
		return result;
	}
	/**
	 * Get the start offset of the match in its version
	 * @return the offset within its version
	 */
	int getStartOffset()
	{
		return offset;
	}
	/**
	 * Get the length of the match in bytes
	 * @param mvd the mvd the match belongs to
	 * @return the length of the match 
	 */
	int getLength()
	{
		return length;
	}
	/**
	 * Get the version of this match
	 * @return the version id
	 */
	public short getVersion()
	{
		return version;
	}
	/**
	 *	Concatenate two lists of Match objects
	 *	@param first the first array
	 *  @param second the second array
	 *  @return the concatenated list
	 */
	static Match[] merge( Match[] first, Match[] second )
	{
		Match[] temp = new Match[first.length+second.length];
		for ( int i=0;i<first.length;i++ )
			temp[i] = first[i];
		for ( int i=0;i<second.length;i++ )
			temp[first.length+i] = second[i];
		return temp;
	}
	/**
	 * Set the found flag. We use this to skip over certain matches 
	 * that exist in several locations
	 * @param found true or false
	 */
	public void setFound( boolean found )
	{
		this.found = found;
	}
	/**
	 * Has this match already been found?
	 * @return true if it is
	 */
	public boolean isFound()
	{
		return found;
	}
	/**
	 * This overrides the superclass method. Tell us if two matches 
	 * are equivalent.
	 * @param other the other match to compare with this one
	 * @return true if they are
	 */
	public boolean equals( Match other )
	{
		if ( this.version != other.version 
			|| this.length != other.length 
			|| this.offset != other.offset )
			return false;
		else
			return true;
	}
	/**
	 * Create the header of a Match (which is the whole thing)
	 * @return a String
	 */
	protected String createHeader()
	{
		StringBuffer sb = new StringBuffer();
		sb.append('[');
		String actualShortName = (shortName!=null&&shortName.length()>0)
			?shortName:Short.toString(version);
		sb.append( actualShortName+":" );
		sb.append( "Version "+Integer.toString(version)+":" );
		sb.append( "Offset "+Integer.toString(offset)+":" );
		sb.append( "Length "+Integer.toString(length)+":" );
		sb.append( state.toString() );
		return sb.toString();
	}
	/**
	 * Convert to a string in the form that will be parsed by the 
	 * parsing constructor
	 * @return the match expressed as a String
	 */
	public String toString()
	{
		return createHeader()+"]";
	}
}

