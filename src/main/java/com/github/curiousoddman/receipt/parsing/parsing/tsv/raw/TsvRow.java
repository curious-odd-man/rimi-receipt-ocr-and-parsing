package com.github.curiousoddman.receipt.parsing.parsing.tsv.raw;


import java.math.BigDecimal;

/**
 * level: hierarchical layout (a word is in a line, which is in a paragraph, which is in a block, which is in a page), a value from 1 to 5
 * 1: page
 * 2: block
 * 3: paragraph
 * 4: line
 * 5: word
 * page_num: when provided with a list of images, indicates the number of the file, when provided with a multi-pages document, indicates the page number, starting from 1
 * block_num: block number within the page, starting from 0
 * par_num: paragraph number within the block, starting from 0
 * line_num: line number within the paragraph, starting from 0
 * word_num: word number within the line, starting from 0
 * left: x coordinate in pixels of the text bounding box top left corner, starting from the left of the image
 * top: y coordinate in pixels of the text bounding box top left corner, starting from the top of the image
 * width: width of the text bounding box in pixels
 * height: height of the text bounding box in pixels
 * conf: confidence value, from 0 (no confidence) to 100 (maximum confidence), -1 for all level except 5
 * text: detected text, empty for all levels except 5
 */
public record TsvRow(int level,
                     int pageNum,
                     int blockNum,
                     int paragraphNum,
                     int lineNum,
                     int wordNum,
                     int left,
                     int top,
                     int width,
                     int height,
                     BigDecimal confidence,
                     String text) {
}
