package servlet;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Random;

public class DigitalSignature {

    public static int generatePrivateKey(int phi, int e) throws Exception {
        final int phiConstant = phi;
        int p0=0;
        int p1=1;
        int pi =0;
        int r  =1;
        int result =-1;
        while (r != 0){

            result = phi / e;
            int num=(p0-(p1*result));

            while (num<=0){
                num+=phiConstant;
            }

            pi= num%phiConstant;
            p0=p1;
            p1=pi;
            r= phi % e;
            phi=e;
            e=r;
        }
        if(result == -1)
            throw new Exception();

        int d = p0;
        return d;
    }

    public static int gcd(int x, int y){
        int gcd=1;

        for(int i = 1; i <= x && i <= y; i++)
        {
//returns true if both conditions are satisfied
            if(x%i==0 && y%i==0)
//storing the variable i in the variable gcd
                gcd = i;
        }
        return gcd;
    }

    private static int randPrime(){
        int num = 0;
        Random rand = new Random();
        num = rand.nextInt(1000) + 1;

        while (!isPrime(num)) {
            num = rand.nextInt(1000) + 1;
        }
        return num;
    }
    
    private static boolean isPrime(int inputNum){
        if (inputNum <= 3 || inputNum % 2 == 0)
            return inputNum == 2 || inputNum == 3;
        int divisor = 3;
        while ((divisor <= Math.sqrt(inputNum)) && (inputNum % divisor != 0))
            divisor += 2;
        return inputNum % divisor != 0;
    }

    public static HashMap<String, Integer> generateKeys() throws Exception {

        // generate two random prime numbers p and q. Tip: https://stackoverflow.com/questions/24006143/generating-a-random-prime-number-in-java
        int p=randPrime();
        int q=randPrime();
        // calculate n = p*q
        int n = p*q;
        // calculate phi = (p-1)*(q-1)
        int phi = (p-1)*(q-1);
        
        // compute e: the minimum number that is coprime with phi greater than 1 and lower than phi
        int e =0;
        for (int i =2 ; i<phi;i++){
            if(gcd(i,phi) ==1){
                e=i;
                break;
            }
        }

        int d = generatePrivateKey(phi,e);
       
        // compute d with the Extended Euclidean algorithm

        HashMap<String,Integer> result = new HashMap<>();
        result.put("public",e);
        result.put("private",d);
        result.put("n",n);

        return result;
    }
    
    public static int[] encrypt(String plaintext, int e, int n){

        int[] crypt = new int[plaintext.length()];
        String[] text =plaintext.split("");
       
        for (int i=0;i<text.length;i++){
            int number = (int)text[i].charAt(0);
            
            crypt[i]=BigDecimal.valueOf(number).toBigInteger().pow(e).mod(BigDecimal.valueOf(n).toBigInteger()).intValue();
            //((int)Math.pow(number, e) % n) ;
            
        }
        // plaintext -> each character is converted into a number given by the position of the character in the alphabet

        //for each number from the plaintext compute  ( pow(number, e) ) mod n
        
        return crypt;
    }

    public static String decrypt(int[] ciphertext, int d, int n){

    	int[] res = new int[ciphertext.length];
    	
        for(int i =0; i<ciphertext.length; i++){
        
            res[i]= BigDecimal.valueOf(ciphertext[i]).toBigInteger().pow(d).mod(BigDecimal.valueOf(n).toBigInteger()).intValue();
        }
        // for each number in the ciphertext compute ( pow(number, d) ) mod n

        StringBuffer result = new StringBuffer();
        for(int i =0; i<res.length; i++){

            result.append((char)res[i]);
        }
        //each resulting number is converted into a character assuming that this number is the position of the character in the alphabet

        return result.toString();
    }
}