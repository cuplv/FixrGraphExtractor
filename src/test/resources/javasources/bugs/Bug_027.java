package bugs;

class Bug_027 {
  public void ciccio() {
    // We do not use the throw statement, that result in an error
    new IllegalStateException("Test incklude");
    return;
  }
}
