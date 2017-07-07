import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.Utilities;
import java.awt.Insets;
//This is the main text area of the editor, where all of the programming happens
public class TextAreaPanel extends JPanel {

	//the underlying text component
	JTextPane textArea = new JTextPane();

	//the file that is represented by this TextAreaPanel instance
	File file;

	//whether the file was just created (specifically by handleNew())
	boolean isNew = false;

	boolean changed = true;
	//stores the keywords based on what is passed to it from the editor class
	HashMap<String, HashSet<String>> keywords = new HashMap<String, HashSet<String>>();

	//the style of TextAreaPanel
	//most of it is inherited from Editor, but there are some specifics to this class
	StyleContext styleContext;
	SimpleAttributeSet keywordAttributes;
	SimpleAttributeSet normalAttributes;

	private Color blackTheme = new Color(38,38,38);
	private Color greyTheme = new Color(128, 129, 135);
	private Color whiteTheme = new Color(239,237,230);


	public TextAreaPanel(StyleContext styleContext) {

		setLayout(new BorderLayout());

		Document d = textArea.getDocument();

		add(textArea);

		this.styleContext = styleContext;

		Action beepAction = textArea.getActionMap().get(DefaultEditorKit.beepAction);

		beepAction.setEnabled(false);

		textArea.setParagraphAttributes(styleContext.getStyle("standard"), true);

		normalAttributes = new SimpleAttributeSet();

		normalAttributes.addAttributes(textArea.getInputAttributes());

		setForeground(whiteTheme);

		textArea.setCaretColor(whiteTheme);
		// keywordAttributes.addAttributes(styleContext.getStyle("keywords"));

	}

	public TextAreaPanel(HashMap<String, HashSet<String>> keywords, StyleContext styleContext) {

		this(styleContext);

		this.keywords = keywords;
	}

	public TextAreaPanel(HashMap<String, HashSet<String>> keywords, StyleContext styleContext, File file) {

		this(styleContext);

		this.keywords = keywords;

		this.file = file;
	}

	public void setText(String input) {

		textArea.setText(input);

	}

	public String getText() {

		try {
			return textArea.getDocument().getText(0, textArea.getDocument().getLength());
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	public JTextPane getTextArea() {

		return textArea;

	}

	public void setFile(File f) {

		file = f;

	}

	public File getFile() {

		return file;
	}

	public String getFileName() {

		return file.getName();

	}

	public String getFilePath() {

		return file.getAbsolutePath();
	}

	//sets the keywords with a new HashMap<>
	public void setKeywords(HashMap<String, HashSet<String>> keywords) {

		this.keywords = keywords;

		try {
			updateKeywords();
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
	}


	public void setIsNew(boolean b) {

		isNew = b;
	}

	public boolean getIsNew() {

		return isNew;
	}

	public void setIsChanged(boolean b) {

		changed = b;
	}

	public boolean getIsChanged() {

		return changed;
	}

	//returns either the style associated with a word or null (not a keyword)
	public String getKeywordStyle(String word) {

		for (String key : this.keywords.keySet()) {
			HashSet<String> words = this.keywords.get(key);
			if (words.contains(word)) {
				return key;
			}

		}

		//if not a keyword
		return null;
	}

	//recolors keywords
	//call this after updating keywords
	public void updateKeywords() throws BadLocationException {

		StyledDocument d = textArea.getStyledDocument();

		Matcher m;
		String text = null;
		try {
			text = d.getText(0, d.getLength());
		} catch (BadLocationException e) {
			e.printStackTrace();
		}
		if (!text.isEmpty()) {
			int i = 0;
			if (Character.isAlphabetic(text.charAt(i))) {
				i++;
				while (i < text.length() && Character.isAlphabetic(text.charAt(i))) {
					i++;
				}
				String word = text.substring(0, i);
				System.out.println("word: " + word);
				String style = getKeywordStyle(word);
				if (style != null) {
					d.remove(0, i);
					d.insertString(0, word, styleContext.getStyle(style));
				}
			}
			//regex pattern to find words within the text
			Pattern p = Pattern.compile("(?<=\\W)[a-z]+(?=\\W)");
			m = p.matcher(text);

			System.out.println("finding keywords: ");

			//finding words and coloring them if they are keywords
			while (m.find()) {

				String word = m.group();

				//System.out.println(keywords);
				System.out.println(word);

				//returns the style associated with the keyword or null if the word is not a keyword
				String style = getKeywordStyle(word);
				if (style != null) {
					int start = m.start();
					System.out.println("start: " + start);
					d.remove(start, word.length());
					d.insertString(start, word, styleContext.getStyle(style));
				}

			}

			//this is checking the end of the text for keywords, because the regex pattern will not find it if there isn't whitespace before and after a word
			if (i < text.length()-1) {
				i = text.length() - 1;
				if (Character.isAlphabetic(text.charAt(i))) {
					i--;
					while (Character.isAlphabetic(text.charAt(i))) {
						i--;
					}
					i++;
					String word = text.substring(i, text.length());
					System.out.println("Word at end of string: " + word);
					String style = getKeywordStyle(word);
					if (style != null) {
						//removes and then inserts the string with the correct style
						d.remove(i, word.length());
						d.insertString(i, word, styleContext.getStyle(style));
					}

					else{
						d.remove(i,word.length());
						d.insertString(i, word, styleContext.getStyle("standard"));
					}

				}
			}

			textArea.setStyledDocument(d);
		}

	}

	//sets the listener for updating keywords as the user types
	public void setKeywordListener() {

		AbstractDocument d = (AbstractDocument) textArea.getDocument();

		//creates document filter to check keywords on every insertion of text
		d.setDocumentFilter(new DocumentFilter() {

			@Override
			public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet)
					throws BadLocationException {

				//regex pattern to find words within inserted text
				Pattern p = Pattern.compile("(?<=\\W)[a-z]+(?=\\W)");

				//indexes of where keywords start and end
				ArrayList<Integer> indexes = new ArrayList<Integer>();

				if (!text.isEmpty()) {
					int i = 0;
					//checking the beginning of the text for keywords, because they're not matched by the regex expression
					if (Character.isAlphabetic(text.charAt(i))) {
						i++;
						while (i<text.length() && Character.isAlphabetic(text.charAt(i))) {
							i++;
						}
						String word = text.substring(0, i);
						System.out.println("word: " + word);

						String style = getKeywordStyle(word);
						if (style != null) {
							indexes.add(0);
							indexes.add(i);
						}
					}

					Matcher m = p.matcher(text);

					System.out.println("finding keywords in insertString: ");

					//actual finder of keywords within the text. Finds keywords and then stores the start and end indexes in the indexes ArrayList<>
					while (m.find()) {

						String word = m.group();

						System.out.println(word);

						String style = getKeywordStyle(word);

						if (style != null) {
							int start = m.start();
							System.out.println("start: " + start);
							indexes.add(start);
							indexes.add(m.end());
						}

					}

					if (!indexes.isEmpty()) {
						super.insertString(fb, offset, text.substring(0, indexes.get(0)), attributeSet);
						for (i = 0; i < indexes.size(); i += 2) {

							int first = indexes.get(i);
							int second = indexes.get(i + 1);
							String temp = text.substring(first, second);
							super.insertString(fb, offset + first, temp, styleContext.getStyle(getKeywordStyle(temp)));

							if(i+2<indexes.size()){
								super.insertString(fb, offset + second, text.substring(second, indexes.get(i + 2)),
										attributeSet);
								}

						}

					}

					else {

						super.insertString(fb, offset, text, attributeSet);

					}

				}

			}

			@Override
			public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {

				super.remove(fb, offset, length);

				System.out.println("running remove");

				int startFirst = -1;
				int endFirst = -1;

				int startSecond = -1;
				int endSecond = -1;

				int temp = offset - 1;
				System.out.println("temp: " + temp);

				if (temp>-1 && Character.isAlphabetic(d.getText(temp, 1).charAt(0))) {
					startFirst = Utilities.getWordStart(textArea, temp);
					endFirst = Utilities.getWordEnd(textArea, temp);
				}

				temp = offset;

				if (offset < d.getLength() - 1) {
					if (Character.isAlphabetic(d.getText(temp, 1).charAt(0))) {
						startSecond = Utilities.getWordStart(textArea, temp);
						endSecond = Utilities.getWordEnd(textArea, temp);
					}
				}
				/*
				 * int tempOffset = offset-1;
				 *
				 * StringBuilder sb = new StringBuilder(d.getText(tempOffset,
				 * 1));
				 *
				 * tempOffset--;
				 *
				 * while(Character.isAlphabetic(sb.charAt(0))){ sb.insert(0,
				 * d.getText(tempOffset, 1)); tempOffset--; }
				 *
				 * tempOffset=offset;
				 *
				 * while(tempOffset<=d.getLength()-1 &&
				 * Character.isAlphabetic(sb.charAt(sb.length()-1))){
				 * sb.append(d.getText(tempOffset, 1)); }
				 */

				String style;

				if (startFirst != -1) {
					String firstWord = d.getText(startFirst, endFirst - startFirst);
					System.out.println("first word: " + firstWord);
					style = getKeywordStyle(firstWord);
					if (style != null) {

						super.remove(fb, startFirst, endFirst - startFirst);
						super.insertString(fb, startFirst, firstWord, styleContext.getStyle(style));
					}

					else {
						super.remove(fb, startFirst, endFirst - startFirst);
						super.insertString(fb, startFirst, firstWord, styleContext.getStyle("standard"));
					}
				}
				if (startSecond != -1) {
					String secondWord = d.getText(startSecond, endSecond - startSecond);
					style = getKeywordStyle(secondWord);
					System.out.println("second word: " + secondWord);
					if (style != null) {
						super.remove(fb, startSecond, endSecond - startSecond);
						super.insertString(fb, startSecond, secondWord, styleContext.getStyle(style));
					} else {
						super.remove(fb, startSecond, endSecond - startSecond);
						super.insertString(fb, startSecond, secondWord, styleContext.getStyle("standard"));
					}

				}

				textArea.setCaretPosition(offset);

			}

			@Override
			public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attributeSet)
					throws BadLocationException {

				int originalOff = offset;
				int originalLength = text.length();

				if (attributeSet.getAttributeCount() > 0) {
					attributeSet = normalAttributes;
				}

				Pattern p = Pattern.compile("^[a-zA-Z]");
				int tempOffset = offset;

				if (tempOffset > 0) {
					String before = d.getText(offset - 1, 1);
					System.out.println("before: " + before);

					if (Character.isAlphabetic(before.charAt(0))) {
						text = before + text;
						offset--;
						tempOffset--;
						length++;
					}
				}
				System.out.println("hello");

				if(offset+length<d.getLength()-1){
					String after = d.getText(offset + length, 1);
					System.out.println("after: " + after);
					if (Character.isAlphabetic(after.charAt(0))) {
						text += after;
						length++;
					}
				}
				System.out.println("hello2");

					if (tempOffset > 0) {
						if (Character.isAlphabetic(text.charAt(0))) {

							tempOffset--;

							StringBuilder sb = new StringBuilder(d.getText(tempOffset, 1));

							tempOffset -= 1;

							while (tempOffset>=0 && Character.isAlphabetic(sb.charAt(0))) {


								sb.insert(0, d.getText(tempOffset, 1));

								tempOffset -= 1;
							}

							offset -= sb.length();

							length += sb.length();

							text = sb.toString() + text;

						}
					}

					System.out.println("hello3");

					if (offset + length < d.getLength() - 1) {
						if (Character.isAlphabetic(text.charAt(text.length() - 1))) {

							StringBuilder sb = new StringBuilder(d.getText(offset + length, 1));
							while (offset + length <= d.getLength() - 1
									&& Character.isAlphabetic(sb.charAt(sb.length() - 1))) {

								length += 1;
								sb.append(d.getText(offset + length, 1));

							}

							sb.deleteCharAt(sb.length() - 1);

							// sb.deleteCharAt(sb.lastIndexOf("\n"));
							System.out.println("sb: " + sb.toString());
							text += sb.toString();
						}
					}


					System.out.println("text");
				System.out.println("text: " + text);

				super.remove(fb, offset, length);

				ArrayList<Integer> indexes = new ArrayList<Integer>();

				if (!text.isEmpty()) {

					int i = 0;

					if (Character.isAlphabetic(text.charAt(i))) {
						i++;
						while (i < text.length() && Character.isAlphabetic(text.charAt(i))) {
							i++;
						}
						String word = text.substring(0, i);
						System.out.println("first word: " + word);
						if (getKeywordStyle(word) != null) {
							indexes.add(0);
							indexes.add(i);
						}
					}

					p = Pattern.compile("(?<=\\W)[a-z]+(?=\\W)");
					Matcher m = p.matcher(text);

					System.out.println("finding keywords in replace: ");

					while (m.find()) {

						String word = m.group();

					//	System.out.println(keywords);
						System.out.println("word: " + word);

						if (getKeywordStyle(word) != null) {
							System.out.println("keyword");
							int start = m.start();
							System.out.println("start: " + start);
							indexes.add(start);
							indexes.add(m.end());
							System.out.println(start);
							System.out.println(m.end());
						}

					}

					int j = text.length() - 1;

					if (i < j && Character.isAlphabetic(text.charAt(j))) {
						j--;

						while (j > 0 && Character.isAlphabetic(text.charAt(j))) {
							j--;
						}

						String word = text.substring(j + 1);
						System.out.println("last word: " + word);
						if (getKeywordStyle(word) != null) {
							indexes.add(j + 1);
							indexes.add(text.length());
						}
					}

					if (!indexes.isEmpty()) {
						super.insertString(fb, offset, text.substring(0, indexes.get(0)), attributeSet);
						for (i = 0; i < indexes.size(); i += 2) {

							int first = indexes.get(i);
							int second = indexes.get(i + 1);

							String temp = text.substring(first, second);

							super.insertString(fb, offset + first, temp, styleContext.getStyle(getKeywordStyle(temp)));

							if (i + 2 < indexes.size()) {
								super.insertString(fb, offset + second, text.substring(second, indexes.get(i + 2)),
										attributeSet);
							}
						}

						super.insertString(fb, offset + indexes.get(indexes.size() - 1),
								text.substring(indexes.get(indexes.size() - 1)), attributeSet);

					}

					else {

						super.insertString(fb, offset, text, attributeSet);

					}
					System.out.println("Setting caret position to " + (originalOff + originalLength));
					textArea.setCaretPosition(originalOff + originalLength);
					System.out.println("end of update");
				}

			}

		});

	}

	public void paintComponent(Graphics g) {

		super.paintComponent(g);

	}

}
