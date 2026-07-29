<?php
namespace App\ViewModel;

use App\Model\EnglishWord;

/** 英语单词视图模型。 */
class EnglishWordViewModel {
    public static function fromArray(array $a): EnglishWord { return EnglishWord::fromUploadArray($a); }
    public static function toArray(EnglishWord $m): array { return $m->toApiArray(); }
    public static function listToArray(array $list): array {
        return array_map(static fn(EnglishWord $m) => $m->toApiArray(), $list);
    }
}
