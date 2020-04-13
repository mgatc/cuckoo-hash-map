package cuckoohashmapdriver;

import java.util.*;

public class CuckooHashMap<K,V> {
    protected static final int DEFAULT_CAPACITY = 4; // from specifications
    protected static final int DEFAULT_STARTING_PRIME = 10007; // from specifications 
    protected static final boolean IS_PRODUCTION = true; // true to suppress diagnostic printing
    
    public Entry<K,V>[][] map = new Entry[2][];
    
    public int prime;
    public int size = 0;
    public int startCapacity;
    protected boolean remapping = false;

    public CuckooHashMap( int cap, int prime ) {
        // Set properties
        cap += cap % 2; // If capacity is odd, cap%2 will be 1. If even, cap%2 will be 0.
        if( cap < CuckooHashMap.DEFAULT_CAPACITY ) // Force the starting capacity to be at least some constant
            cap = CuckooHashMap.DEFAULT_CAPACITY;
        this.prime = prime;
        //System.out.println(this.prime);
        this.startCapacity = cap;
        // Make sure arrays are big enough
        for( int i=0; i<this.map.length; i++ ) 
            this.map[i] = new Entry[ cap/this.map.length ];
        diagnostics();
    }
    public CuckooHashMap( int cap ) {
        this( cap, CuckooHashMap.DEFAULT_STARTING_PRIME );
    }
    public CuckooHashMap() {
        this( CuckooHashMap.DEFAULT_CAPACITY, CuckooHashMap.DEFAULT_STARTING_PRIME );
    }
    
    /**
     * Public methods
     */
    public float loadFactor() {
        return ( (float) this.size ) / this.capacity();
    }
    public int size() {
        return this.size;
    }
    public int capacity() {
        int cap = 0;
        for( Entry[] t: this.map )
            cap += t.length;
            
        return cap;
    }
    public V put( K key, V value ) {
        Entry e = new Entry( key, value );
        Entry f = placeEntry( e, 0 );
        V returnVal;
        
        if( f == null ) { // entry placed, not an update
            returnVal = null;
            this.size++; // only change the size when we insert a NEW key
        } else            // either an update or a loop condition
            if( f.getKey() == null ) { // if the key is null, loop condition
                rehash();
                returnVal = put( key, value ); // try again
            } else // update
                returnVal = (V) f.getValue();
        
        if( !this.remapping && this.needsResize() )
            this.resize( Math.round( this.resizeFactor() * this.capacity() ) );
        
        return returnVal;
    }
    public V get( K key ) {
        int location = this.findEntry( key );
        
        diagnostics();
        if( location >= 0 )
            return this.map
                [ location / this.capacity() ]
                    [ location % this.capacity() ]
                        .getValue();
        else
            return null;
    }
    public V remove( K key ) {
        int location = this.findEntry( key );
        V value = null;
        if( location >= 0 ) {
            value = this.map
                [ location / this.capacity() ]
                    [ location % this.capacity() ]
                        .getValue();
            this.map
                [ location / this.capacity() ]
                    [ location % this.capacity() ] = null;
        }
        this.size--;
        
        diagnostics();
        
        return value;
    }
    public Iterable<Entry<K,V>> entrySet() {
        if( this.size() == 0 )
            return new ArrayList();
        
        ArrayList<Entry<K,V>> iteratee = new ArrayList();
        int j=0;
        
        for( int i=0; i<this.capacity(); i++ ) 
            if( this.map[ i%2 ][ i/2 ] != null ) {
                Entry e = this.map[ i%2 ][ i/2 ];
                iteratee.add( new Entry( e.getKey(), e.getValue() ) );
            }
        
        return iteratee;
    }
    public Iterable<K> keySet() {
        if( this.size() == 0 )
            return new ArrayList();
        
        ArrayList<K> iteratee = new ArrayList();
        int j=0;
        
        for( int i=0; i<this.capacity(); i++ )
            if( this.map[ i%2 ][ i/2 ] != null )
                iteratee.add( this.map[ i%2 ][ i/2 ].getKey() );
                
        return iteratee;
    }
    public Iterable<V> valueSet() {
        if( this.size() == 0 )
            return new ArrayList();
        
        ArrayList<V> iteratee = new ArrayList();
        int j=0;
        
        for( int i=0; i<this.capacity(); i++ )
            if( this.map[ i%2 ][ i/2 ] != null )
                iteratee.add( this.map[ i%2 ][ i/2 ].getValue() );
        return iteratee;
    }
    
    /**
     * Private methods from specifications
     */
    private void rehash() {
        this.remapping = true;
        if( !CuckooHashMap.IS_PRODUCTION )
            System.out.println( "Rehashing..." );
    
        getNextPrime();
        
        // create an iterable arraylist of entries in the map
        ArrayList<Entry<K,V>> entries = (ArrayList)this.entrySet();
        
        // clear the map
        this.map = new Entry[ this.map.length ][ this.capacity()/this.map.length ];
        
        // put each entry back in the map
        for( Entry e: entries ) {
            put( (K) e.getKey(), (V) e.getValue() );
            size--;
        }
        this.remapping = false;
    }
    private void resize( int newCap ) {
        this.remapping = true;
        
        if( !CuckooHashMap.IS_PRODUCTION )
            System.out.println( "Resizing..." + newCap + " " + this.size() + " " + this.capacity() );
        
        // create an iterable arraylist of entries in the map
        ArrayList<Entry<K,V>> entries = (ArrayList) this.entrySet();
        // Make this.map point to the empty map structure (can handle more than just two nests)
        this.map = new Entry[ this.map.length ][ newCap/this.map.length ];
        this.size = 0;
        
        // put each entry back in the map
        for( Entry e: entries ) {
            put( (K) e.getKey(), (V) e.getValue() );
        }
        
        rehash();
    }
    private int h1( K key ) {
        return ( Math.abs(key.hashCode() ) % this.prime ) % ( this.capacity() / 2 );
    }
    private int h2( K key ) {
        return ( ( Math.abs( key.hashCode() ) / this.prime ) % this.prime ) % ( this.capacity() / 2 );
    }
    
    /**
     * Private methods from student
     */
    private Entry placeEntry( Entry e, int i ) {
        if( i > this.size )   // loop condition detected
            return new Entry(); // signifies the loop condition
        
        int table = i%2;    // determine which map to look and hash to use
        K key = (K) e.getKey();
        int hash = this.hash( key, table );
        
        Entry old = this.map[ table ][ hash ]; 
        
        if( old == null ) { // no update, no relocation
            this.map[ table ][ hash ] = e; // update the value in the table
            return null;
        } else {
            old = new Entry( old.getKey(), old.getValue() ); // make sure we're not pointed at the object in the map
            this.map[ table ][ hash ] = e; // update the value in the table

            if( old.getKey() == e.getKey() ) // update (should only happen during the initial call to placeEntry())
                return new Entry( old.getKey(), old.getValue() ); 
            else // relocation
                return placeEntry( old, ++i ); 
        }
    }
    private int findEntry( K key ) {
        int hash = h1( key );
        
        if( this.map[0][ hash ] != null && this.map[0][ hash ].getKey() == key )
            return hash;
        else {
            hash = h2( key );
            if( this.map[1][ hash ] != null && this.map[1][ hash ].getKey() == key )
                return hash + this.capacity();
        }
        return -1;
    }
    private int getNextPrime() {
        return LazyPrime.next( this.prime );
    }
    private boolean needsResize() {
        return 
                this.loadFactor() > 0.5 
            || 
                    this.loadFactor() < 0.25 
                && 
                    this.capacity() > this.startCapacity;
    }
    private float resizeFactor() {
        float l = this.loadFactor();
        if( l < 0.25 )
            return (float)0.5;
        else if( l > 0.5 )
            return 2;
        else
            return 1;
    }
    private int hash( K key, int function ) {
        function = function % this.map.length; // make sure the function number is valid
        switch( function ) {
            case 0:
                return this.h1( key );
            case 1:
                return this.h2( key );
        }
        return -1;
    }
    private void diagnostics() {
        if( CuckooHashMap.IS_PRODUCTION )
            return;
        
        String out = "------------\n";
        out       += "Diagnostics:\n";
        out       += "------------\n";
        out       += "capacity(): " + this.capacity() + "\n";
        out       += "loadFactor(): " + this.loadFactor() + "\n";
        out       += "size(): " + this.size() + "\n";
        out       += "needsResize(): " + this.needsResize() + "\n\n";
        out       += "Entries:\n";

        for( Entry e: entrySet() )
            out   += " - " + e.getKey() + " " + e.getValue() + "\n";
        
        System.out.println(out);
    }
    
    /**
     * Static nested class for calculating primes
     */
    private static class LazyPrime {
        // Static fields
        public static final int N = 100; // starting ArrayList size
        public static int n = LazyPrime.N; // ArrayList size
        // Sieve of Eratosthenes
        public static ArrayList<Boolean> primes = new ArrayList<>( 
            Collections.nCopies( LazyPrime.N, true ) 
        );
        // Instance fields
        public int j = 2;
        public int max;

        // Constructors
        public LazyPrime( int max ) {
            if( !LazyPrime.isInitialized() )
                LazyPrime.init();
            this.max = max;
        }
        public LazyPrime() {
            this( 1000 );
        }

        // Static methods
        public static void init() {
            if( !isInitialized() ) {
                primes.set( 0, false ); // initialized flag
            }
        }
        public static boolean isInitialized() {
            // During initialization we set 0 and 1 to false
            return !primes.get(0);
        }
        // Return next prime using the Sieve of Eratosthenes, -1 for error
        public static int sieve( int f ) {
            // set all elements with indices divisable by f (current prime) to false
            for( int j=f+f; j<n; j+=f ) {
                if( primes.get(j) ) {
                    //System.out.println( "  " +j + " " + f);
                    primes.set( j, false );
                    //System.out.println("remov:"+j);
                }
            }
            //System.out.println(primes.get(f));
            f++; // go to next element
            while( f<n && !primes.get(f) )
                f++;//System.out.println("notpr:"+ f++);
            if( f<n )
                return f;
            else // We've reached capacity
                return -1;
        }
        protected static boolean increaseCapacity() {
            int j = 2;
            try {
                primes.addAll( Collections.nCopies( n, true ) ); // fill new candidates w/ true
                n *= 2; // double max prime
            } catch( Exception e ) {
                return false;
            }
            while( 1 < j && j < n/2 ) {
                j = LazyPrime.sieve( j );
                //System.out.println(j);
            }
            return j > 1;
        }
        public static int next( int current ) {
            if( !LazyPrime.isInitialized() )
                LazyPrime.init();
            if( current > 1 )
                current = LazyPrime.sieve( current );
            return current;
        }
        public static boolean hasNext( int current, int max ) {
            if( !LazyPrime.isInitialized() )
                LazyPrime.init();
            int next = next( current );
            if( next > 1 && next < max )
                return true;
            else
                while( LazyPrime.n < current )
                    LazyPrime.increaseCapacity();

            next = next( current );
            if( next > 1 && next < max )
                return true;
            else
                return false;
        }
        public static boolean isPrime( int num ) {
            LazyPrime p;
            if( num < 2 )
                return false;
            if( num == 2 )
                return true;
            if( num > LazyPrime.n ) {
                p = new LazyPrime(num+1);
                while( p.hasNext() )
                    if( p.next() == num )
                        return true;
                return false;
            } // need to loop through existing primes in sieve
            return false;
        }

        // Instance methods
        public boolean hasNext() {
            if( this.j < this.max-1 ) 
                return hasNext( this.j, this.max );
            else
                return false;
        }
        public int next() {
            if( primes.get(1) ) { // If 1 is true, we haven't used 2 yet
                primes.set( 1, false ); // set 1 to false
                return 2;               // return f (2)
            }
            if( j > 1 )
                this.j = LazyPrime.sieve( this.j );
            return this.j;
        }
    }
}
