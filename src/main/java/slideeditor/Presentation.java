package slideeditor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Presentation implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Slide> slides;
    private String title;
    
    public Presentation() {
        this.slides = new ArrayList<>();
        this.title = "Untitled Presentation";
        slides.add(new Slide());
    }
    
    public void addSlide() {
        slides.add(new Slide());
    }
    
    public void addSlide(int index) {
        slides.add(index, new Slide());
    }
    
    public void removeSlide(int index) {
        if (slides.size() > 1) {
            slides.remove(index);
        }
    }
    
    public void moveSlide(int fromIndex, int toIndex) {
        if (fromIndex >= 0 && fromIndex < slides.size() && toIndex >= 0 && toIndex < slides.size()) {
            Slide slide = slides.remove(fromIndex);
            slides.add(toIndex, slide);
        }
    }
    
    public Slide getSlide(int index) {
        return slides.get(index);
    }
    
    public List<Slide> getSlides() {
        return slides;
    }
    
    public int getSlideCount() {
        return slides.size();
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
}