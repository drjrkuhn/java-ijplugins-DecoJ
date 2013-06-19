DecoJ and DecoJNA
=================

Iterative batch deconvolution software for ImageJ
-------------------------------------------------

_Batch deconvolution software for ImageJ - by me (Jeff Kuhn). 
Both Windows, Macintosh (Lion), and Linux-64bit version are included. 
I have tested the mac version on Lion, the Linux version on Ubunty 12.04 64-bit 
and the Windows Version on Windows 7 (32 and 64 bit)._

**Last updated 2012-07-26**

### Description and background

The software relies on fftw3 for 3D Fourier transforms. It must be installed
either with "apt-get install fftw3*" on linux, "port install fftw3*" on OSX,
or by downloading the appropriate windows DLL files from fftw.org.

The software was originialy written as a client-server platform. The server
did the actual work of the deconvolution while the client ran on ImageJ
and fed stacks to the server. The client-server model was reworked using JNA
to allow Java and C code to talk to each other. This sped the transfer of
stacks back-and-forth, but precluded the eventual port of the code to a true
client-server model with the deconvolution server running on a separate machine
or cluster.

The ImageJ portion of the code was written before ImageJ had hyperstacks or
virtual stacks. Thus, the code treats all stacks as XYZ-WL-T. Thus, it reads
through each stack and splits it into temporary files containing one 3D scan
each. It chooses the PSF to use for deconvolution based on the order you set
in the options file. If there are still stack planes after iterating through
the list of wavelengths, it assumes that each is now a new timepoint and
starts the process of writing temporary 3D files again. Once the stack has
been split, the ImageJ client feeds the stacks one-by-one to the deconvolution
server. I would love to rewrite the client to take advantage of hyperstacks,
but I just don't have the time. If anyone else wants to tackle this, please
feel free.

Batch processing is handled by the options editor and batch processor plugin
menu items in ImageJ. Crop your stacks down to the size you want to deconvolve
and place the set of files in a subfolder. Then use the options editor to
create a file called "allstacks.dop" in that subfolder. The options file tells
the batch processor where to find the measured PSF files for each wavelength.
See below for more instructions.

### Notes of FFTW3

FFTW (http://fftw.org/) is a fantastic platform for doing deconvolution. Not
only can it handle sizes that are powers of 2 (2^n), but it does prime-factor
algorithms as well. In other words, image dimensions can be any multiple of
primes or powers of 2, 3, 5, and 7. For example, an image can be 7x5x8 = 280 
pixels on a side.

Here is a list of some prime factors of 2, 3, 5, and 7 that can be used
as x, y, and z dimensions for deconvolution:

2
3	5	6	7	9	10	12	14	15	18	
20	21	24	25	27	28	30	35	36	40	
42	45	48	49	50	54	56	60	63	70	
72	75	80	84	90	96	98	100	105	108	
112	120	125	126	135	140	144	147	150	160	
168	175	180	189	192	196	200	210	216	224	
225	240	245	250	252	270	280	288	294	300	
315	320	336	343	350	360	375	378	384	392	
400	420	432	441	448	450	480	490	500	504	
525	540	560	576	588	600	630	640	672	675	
686	700	720	735	750	756	768	784	800	840	
864	875	882	896	900	945	960	980	1000	1008	
1029	1050	1080	1120	1125	1152	1176	1200	1225	1260	
1280	1323	1344	1350	1372	1400	1440	1470	1500	1512	
1536	1568	1575	1600	1680	1715	1728	1750	1764	1792	
1800	1890	1920	1960	2000	2016	2058	2100	2160	2205	
2240	2250	2304	2352	2400	2450	2520	2560	2625	2646	
2688	2700	2744	2800	2880	2940	3000	3024	3087	3136	
3150	3200	3360	3375	3430	3456	3500	3528	3584	3600	
3675	3780	3840	3920	4000	4032	4116	4200	4320	4410	
4480	4500	4608	4704	4725	4800	4900	5040	5120	5145	
5250	…	


###  Installing DecoJ

  1. Download and install [ImageJ][1]
  2. Download the&nbsp;[DecoJ-plugin.zip][2]&nbsp;(3,3 MB) package and unzip it.
  3. Place the _contents_ of this folder into the ImageJ plugins folder. For example, your folder hierarchy should be:
ImageJ/plugins/DecoJ_Client.jar
ImageJ/plugins/DecoJNA.dll
ImageJ/plugins/lib/...
ImageJ/plugins/libDecoJNA.dylib
...
  4. Run ImageJ.
  5. Select "**Plugins &gt; DecoJ &gt; Server Information"**&nbsp;to test the installation.

NOTE: The windows versions of the [fftw DLLs][3] are included with DecoJ package.

####  ​Additional Installation instructions for Linux

  1. Open a terminal window to get a command prompt
  2. Use the package manager to install libfftw3
**sudo apt-get install libfftw3**

####  ​Additional Installation instructions for Mac OS-X

  1. Install [MacPorts][4]&nbsp;().
  2. Open a terminal window to get a command prompt
  3. Use the package manager to install fftw-3
**sudo port install fftw-3**

###  To make PSFs

  1. Take a stack of bead images.
  2. Draw a small box around a bead.
  3. Choose **Plugins &gt; DecoJ &gt; Auto Shift and Crop PSF**.
  4. Do this for a number of PSFs.
  5. Add several PSFs together via ImageJ's "**Process &gt; Image Calculator...**"
  6. Divide by the number of PSFs you added together via "**Process &gt; Math &gt; Divide...**"
  7. Save this PSF

###  To batch process stacks

  1. Create an empty folder to contain the images to deconvolve.
  2. Use "**Image &gt; Duplicate**" to crop out a portion of your image stack to deconvolve.
  3. Save the image to that folder. NOTE: DecoJ expects the image stacks to be sorted by Z-plane, then Color, then Time.
  4. Run "**Plugins&nbsp;&gt;&nbsp;DecoJ&nbsp;&gt;&nbsp;Options Editor...**"
  5. Type in the number of itterations.
  6. Type in the number of Z planes in your stack.
  7. Select the number of wavelengths. DecoJ uses the number of Z planes and number of wavelengths to parse large stacks into individual components for deconvolution.
  8. For each wavelength, point to the PSF file.
NOTE: The directory will likely be different for each computer you run DecoJ on, so make sure and point to the correct PSF file.
  9. Save the "allstacks.dop" file to the folder with the images you selected for processing above. Close the options editor. DecoJ will try to deconvolve every file in a folder containing the "allstacks.dop" file. Make sure the folder just contains image stacks and the ".dop" file.
  10. Run "**Plugins&nbsp;&gt;&nbsp;DecoJ&nbsp;&gt;&nbsp;Deconvolve Batch**" and select the "allstacks.dop" file you just created.
  11. DecoJ will show you a list of files to deconvolve.
  12. Approve the operation, sit back, and wait.
NOTE: If you quit ImageJ or close the ImageJ main window, deconvolution will STOP. Be careful.
  13. DecoJ creates a subfolder named "out" to store the deconvolved stacks. You can peek at them in ImageJ as they come out, but do not do too much image processing while DecoJ is running in the background.
  14. I suggest a test run with 10 or so iterations to try it out.

_NOTE:_ I have included some&nbsp;[Test Data][5]&nbsp;(95.4 MB) of a large bead and a PSF to make sure everything is working. You will have to change the location of the PSF file in the "allstacks.dop" example file. You can load it with the options editor, modify it, and save it. You can also edit options file with a simple text editor.

###  GOOD LUCK.

  * Let me know if you have any problems.
  * The source code is available upon request if you want to try compiling it or modifying it. Note that I have made several changes over the years and the source by now has become somewhat "cobbled together". The whole thing needs a major rewrite if I ever have the time.

   [1]: http://rsbweb.nih.gov/ij/
   [2]: http://www.faculty.biol.vt.edu/kuhn/sites/default/files/software/DecoJ-plugin.zip
   [3]: http://www.fftw.org/install/windows.html
   [4]: http://www.macports.org/
   [5]: http://www.faculty.biol.vt.edu/kuhn/sites/default/files/software/TestData.zip
  