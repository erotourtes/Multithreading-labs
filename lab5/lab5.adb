-- Program header: «ПРОГРАМНЕ ЗАБЕЗПЕЧЕННЯ ВИСОКОПРОДУКТИВНИХ КОМП’ЮТЕРНИХ СИСТЕМ.»
-- Lab 5: «Програмування для комп’ютерних систем з локальною пам’яттю»
-- Function:
-- - a=min(C*MZ)+max(D*(MX*MR))
-- Name: Сірик Максим Олександрович
-- Date: 21.04.2025

with Ada.Text_IO;
with Ada.Strings.Fixed;
use Ada;

procedure Lab5 is
   P : Integer := 6;
   N : Integer := P * 300;
   H : Integer := N / P;

   type Vector is array (Positive range <>) of Integer;
   subtype VectorN is Vector (1 .. N);
   subtype VectorH is Vector (1 .. H);

   type Matrix is array (Positive range <>, Positive range <>) of Integer;
   subtype MatrixNxN is Matrix (1 .. N, 1 .. N);
   subtype MatrixNxH is Matrix (1 .. N, 1 .. H);

   function mulVxMA (V : VectorN; MA : MatrixNxH) return VectorH is
      result : VectorH;
   begin
      for i in 1 .. MA'Length (2) loop
         result (i) := 0;
         for j in 1 .. MA'Length (1) loop
            result (i) := result (i) + V (i) * MA (j, i);
         end loop;
      end loop;

      return result;
   end mulVxMA;

   function mulMAxMA (MA : MatrixNxN; MB : MatrixNxH) return MatrixNxH
   with Pre => MA'Length (2) = MB'Length (1)
   is
      result : MatrixNxH := (others => (others => 0));
   begin
      for i in 1 .. MA'Length (1) loop
         for j in 1 .. MA'Length (2) loop
            for k in 1 .. MB'Length (2) loop
               result (i, k) := result (i, k) + MA (i, j) * MB (j, K);
            end loop;
         end loop;
      end loop;

      return result;
   end mulMAxMA;

   function minV (V : VectorH) return Integer is
      min : Integer := V (V'First);
   begin
      for i in V'Range loop
         min := Integer'Min (min, V (i));
      end loop;

      return min;
   end minV;

   function maxV (V : VectorH) return Integer is
      max : Integer := V (V'First);
   begin
      for i in V'Range loop
         max := Integer'Max (max, V (i));
      end loop;

      return max;
   end maxV;

   function getSubmatrix (MA : MatrixNxN; part : Integer) return MatrixNxH is
      colFirst : Integer := H * (part - 1) + 1;
      colLast  : Integer := H * part;
      result   : MatrixNxH;
   begin
      for i in 1 .. N loop
         for j in colFirst .. colLast loop
            result (i, j - colFirst + 1) := MA (i, j);
         end loop;
      end loop;

      return result;
   end getSubmatrix;

   function getValue return Integer is
   begin
      return 1;
   end getValue;

   function format (val : Integer) return String is
   begin
      return Strings.Fixed.Trim (Integer'Image (val), Strings.Left);
   end format;

   task T1 is
      pragma Storage_Size (73000000);
      entry Data
        (MZh_in : in MatrixNxH;
         D_in   : in VectorN;
         MX_in  : in MatrixNxN;
         MRh_in : in MatrixNxH);
   end T1;

   task T2 is
      pragma Storage_Size (73000000);
      entry T1Data (C_in : in VectorN);
      entry Result (Oh : in VectorH; Mh : in VectorH);
      entry T3Data
        (MZ_in : in MatrixNxN; D_in : in VectorN; MR_in : in MatrixNxN);
   end T2;

   task T3 is
      pragma Storage_Size (73000000);
      entry Data (C_in : in VectorN; MX_in : in MatrixNxN);
   end T3;

   task type TR (ThreadNum : Integer) is
      pragma Storage_Size (73000000);
      entry Data
        (C_in   : in VectorN;
         MX_in  : in MatrixNxN;
         MZh_in : in MatrixNxH;
         D_in   : in VectorN;
         MRh_in : in MatrixNxH);
   end TR;

   TRArray : array (4 .. P) of access TR;

   task body T1 is
      C, D     : VectorN;
      MX       : MatrixNxN;
      MZh, MRh : MatrixNxH;
      Oh, Mh   : VectorH;
   begin
      Text_IO.Put_Line ("T1 is started");

      -- Уведення C
      for i in 1 .. C'Length loop
         C (i) := getValue;
      end loop;

      -- Передати задачі T2
      T2.T1Data (C);

      -- Прийняти від T2
      accept Data
        (MZh_in : MatrixNxH;
         D_in   : VectorN;
         MX_in  : MatrixNxN;
         MRh_in : MatrixNxH)
      do
         MZh := MZh_in;
         D := D_in;
         MX := MX_in;
         MRh := MRh_in;
      end Data;

      -- Обчислення
      Oh := mulVxMA (C, MZh);
      Mh := mulVxMA (D, mulMAxMA (MX, MRh));

      -- Передати Т2
      T2.Result (Oh, Mh);

      Text_IO.Put_Line ("T1 is ended");
   end T1;

   task body T2 is
      MX, MZ, MR : MatrixNxN;
      C, D       : VectorN;
      Oh, Mh     : VectorH;
      o, m, a    : Integer;
   begin
      Text_IO.Put_Line ("T2 is started");

      -- Уведення MX
      for i in 1 .. C'Length loop
         for j in 1 .. C'Length loop
            MX (i, j) := getValue;
         end loop;
      end loop;

      -- Прийняти від T1
      accept T1Data (C_in : VectorN) do
         C := C_in;
      end T1Data;

      -- Прийняти від T3
      accept T3Data (MZ_in : MatrixNxN; D_in : VectorN; MR_in : MatrixNxN) do
         MZ := MZ_in;
         D := D_in;
         MR := MR_in;
      end T3Data;

      -- Передати задачі T1
      T1.Data
        (MZh_in => getSubmatrix (MZ, 1),
         D_in   => D,
         MX_in  => MX,
         MRh_in => getSubmatrix (MR, 1));

      -- Передати задачі T3
      T3.Data (C_in => C, MX_in => MX);

      -- Передати задачам T4-T6
      for i in TRArray'Range loop
         TRArray (i) := new TR (i);
         TRArray (i).Data
           (C_in   => C,
            MX_in  => MX,
            D_in   => D,
            MZh_in => getSubmatrix (MZ, i),
            MRh_in => getSubmatrix (MR, i));
      end loop;

      -- Обчислення
      Oh := mulVxMA (C, getSubmatrix (MZ, 2));
      Mh := mulVxMA (D, mulMAxMA (MX, getSubmatrix (MR, 2)));
      o := minV (Oh);
      m := maxV (Mh);

      -- Прийняти Oн, Mн
      for i in 1 .. P - 1 loop
         accept Result (Oh : in VectorH; Mh : in VectorH) do
            o := Integer'Min (o, minV (Oh));
            m := Integer'Max (m, maxV (Mh));

         end Result;
      end loop;

      a := o + m;
      Text_IO.Put_Line ("Result is " & format (a));

      Text_IO.Put_Line ("T2 is ended");
   end T2;

   task body T3 is
      C, D       : VectorN;
      MR, MZ, MX : MatrixNxN;
      Oh, Mh     : VectorH;
   begin
      Text_IO.Put_Line ("T3 is started");

      -- Уведення MR, MZ, D
      for i in 1 .. N loop
         D (i) := getValue;
         for j in 1 .. N loop
            MR (i, j) := getValue;
            MZ (i, j) := getValue;
         end loop;
      end loop;

      -- Передати задачі T2
      T2.T3Data (MZ_in => MZ, D_in => D, MR_in => MR);

      -- Прийняти від T2
      accept Data (C_in : in VectorN; MX_in : in MatrixNxN) do
         C := C_in;
         MX := MX_in;
      end Data;

      -- Обчислення
      Oh := mulVxMA (C, getSubmatrix (MZ, 3));
      Mh := mulVxMA (D, mulMAxMA (MX, getSubmatrix (MR, 3)));

      -- Передати Т2
      T2.Result (Oh, Mh);

      Text_IO.Put_Line ("T3 is ended");
   end T3;

   task body TR is
      C   : VectorN;
      MX  : MatrixNxN;
      MZh : MatrixNxH;
      D   : VectorN;
      MRh : MatrixNxH;

      Oh : VectorH;
      Mh : VectorH;
   begin
      Text_IO.Put_Line ("T" & format (ThreadNum) & " is started");

      -- Прийняти від T2
      accept Data
        (C_in   : VectorN;
         MX_in  : MatrixNxN;
         MZh_in : MatrixNxH;
         D_in   : VectorN;
         MRh_in : MatrixNxH)
      do
         C := C_in;
         MX := MX_in;
         MZh := MZh_in;
         D := D_in;
         MRh := MRh_in;
      end Data;

      -- Обчислення
      Oh := mulVxMA (C, MZh);
      Mh := mulVxMA (D, mulMAxMA (MX, MRh));

      -- Передати Т2
      T2.Result (Oh, Mh);

      Text_IO.Put_Line ("T" & format (ThreadNum) & " is ended");
   end TR;

begin
   Text_IO.Put_Line ("Main is started");
end Lab5;
