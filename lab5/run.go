package main

import (
	"flag"
	"fmt"
	"os"
	"os/exec"
	"strings"
	"time"
)

func main() {
	initFlag := flag.Bool("init", false, "Install dependencies")
	cleanFlag := flag.Bool("remove", false, "Clean build files")
	benchmark := flag.Bool("benchmark", false, "Benchmark build")

	flag.Parse()

	if *cleanFlag {
		remove()
		return
	}

	if *initFlag {
		install()
	}

	build()

	if *benchmark {
		runBenchmark()
	} else {
		run()
	}
}

const BUILD_PATH = "./build"

func install() {
	fmt.Println("Installing ada")
	if err := cmd("sudo dnf install -y gcc-gnat").Run(); err != nil {
		fmt.Println("Install failed:", err)
		os.Exit(1)
	}
}

func build() {
	c := cmd("mkdir -p ./build")
	if err := c.Run(); err != nil {
		fmt.Println("Can't create build dir", err)
		os.Exit(1)
	}

	fmt.Println("Compiling ada...")
	// c = cmd(fmt.Sprintf("gnatmake -D %s ./lab5.adb -o %s/main", BUILD_PATH, BUILD_PATH))
	c = cmd(fmt.Sprintf("gnatmake -gnatp -gnatn -fPIC -D %s ./lab5.adb -o %s/main", BUILD_PATH, BUILD_PATH))
	if err := c.Run(); err != nil {
		fmt.Println("Compilation failed\n", err)
		os.Exit(1)
	}

}

func run() {
	fmt.Println("\nRunning ada...")
	c := cmd(fmt.Sprintf("%s/main", BUILD_PATH))
	if err := c.Run(); err != nil {
		fmt.Println("Running failed\n", err)
		os.Exit(1)
	}
}

func runBenchmark() {
	fmt.Println("\nRunning benchmark...")

	runs := 4
	durations := make([]time.Duration, runs)
	var total time.Duration

	var cores string
	if true {
		cores = "0"
	} else {
		cores = "0,1,2,3"
	}

	for i := 0; i < runs; i++ {
		start := time.Now()

		c := cmd(fmt.Sprintf("sudo taskset -c %s nice -n -20 %s/main", cores, BUILD_PATH))
		if err := c.Run(); err != nil {
			fmt.Println("Running failed\n", err)
			os.Exit(1)
		}
		elapsed := time.Since(start)
		durations[i] = elapsed
		total += elapsed
	}
	average := total / time.Duration(runs)
	for i, d := range durations {
		fmt.Printf("Run %d time: %v\n", i+1, d)
	}
	fmt.Printf("Execution time: %v\n", average)
}

func remove() {
	fmt.Println("Removing ada")
	c := cmd("sudo dnf remove -y gcc-gnat")
	if err := c.Run(); err != nil {
		fmt.Println("Install failed:", err)
		os.Exit(1)
	}

	fmt.Printf("Removing %s", BUILD_PATH)
	if err := os.RemoveAll(BUILD_PATH); err != nil {
		fmt.Println("Removal failed:", err)
		os.Exit(1)
	}
}

func cmd(command string) *exec.Cmd {
	parts := strings.Fields(command)
	fmt.Printf("Running: \n%s\n\n", command)
	cmd := exec.Command(parts[0], parts[1:]...)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	cmd.Stdin = os.Stdin
	return cmd
}
